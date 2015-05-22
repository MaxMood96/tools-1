/**
 * Copyright (c) 2014 Gang Ling.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
*/
package org.spdx.merge;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.JavaSha1ChecksumGenerator;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import org.spdx.rdfparser.license.ExtractedLicenseInfo;
import org.spdx.rdfparser.license.LicenseInfoFactory;
import org.spdx.rdfparser.model.SpdxDocument;
import org.spdx.rdfparser.model.SpdxFile;
import org.spdx.rdfparser.model.SpdxPackage;
import org.spdx.rdfparser.SpdxPackageVerificationCode;
import org.spdx.rdfparser.VerificationCodeGenerator;
import org.spdx.spdxspreadsheet.InvalidLicenseStringException;

/**
 * Application to merge package information from input SPDX documents and file information merging result.
 * @author Gang Ling
 *
 */
public class SpdxPackageInfoMerger {

	    @Deprecated
		private SpdxPackage packageInfoResult = null;
	    @Deprecated
		private SpdxDocument[] allDocs;
		
		private List<SpdxPackage> packagesResult = null;
		private SpdxDocument[] subDocs = null;
		private SpdxLicenseMapper mapper = null;
		
		/** 
		 * @param masterPackagesInfo
		 * @param subDocs
		 */
		public SpdxPackageInfoMerger(List<SpdxPackage> masterPackagesInfo, SpdxDocument[] subDocs
				, SpdxLicenseMapper mapper){
			this.packagesResult = masterPackagesInfo;
			this.subDocs = subDocs;
			this.mapper = mapper;
		}
		
		/** A method to merge all packages' information from sub list documents into master document
		 * @param subDocs
		 * @param fileMergeResult
		 * @return packagesResult
		 * @throws InvalidSPDXAnalysisException
		 */
		public List <SpdxPackage> mergePackagesInfo(SpdxDocument[] subDocs, SpdxFile[] fileMergeResult)
				throws InvalidSPDXAnalysisException, NoSuchAlgorithmException, InvalidLicenseStringException{
			
			List<SpdxPackage> retval = new ArrayList<SpdxPackage>(clonePackages(packagesResult));
			
			for(int i = 0; i < subDocs.length; i++){				
				List<SpdxPackage> subPackagesInfo = subDocs[i].getDocumentContainer().findAllPackages();
				SpdxPackage tempPackage = null;
				for(int p = 0; p < subPackagesInfo.size(); p++){
					boolean foundNameMatch = false;
					boolean foundSha1Match = false;
					SpdxPackage masterPackage = null;
					Integer index = null;
	                tempPackage = subPackagesInfo.get(p);
	                
	                for(int q = 0; q < retval.size(); q++){
	                	if(retval.get(q).getName().equalsIgnoreCase(tempPackage.getName()))
	                		foundNameMatch = true;
	                	if(retval.get(q).getSha1().equals(tempPackage.getSha1())){
	                		foundSha1Match = true;
	                		masterPackage = retval.get(q);
	                		index = q;
	                		break;
	                	}
	                }
	                if(!foundNameMatch && !foundSha1Match){
	                	AnyLicenseInfo[] licFromFile = checkLicenseFromFile(subDocs[i],tempPackage);
	                	tempPackage.setLicenseInfosFromFiles(licFromFile);
	                	retval.add(tempPackage);
	                }
	                else{
	                	 AnyLicenseInfo[] masterLicFromFile = masterPackage.getLicenseInfoFromFiles();
	                	 AnyLicenseInfo[] licFromFile = checkLicenseFromFile(subDocs[i],tempPackage);
	                	 ArrayList <AnyLicenseInfo> licList = new ArrayList<AnyLicenseInfo> (Arrays.asList(masterLicFromFile));
	                	 for(int g = 0; g < licFromFile.length; g++){
	                		 for(int d = 0; d < licList.size(); d++){
	                			 if(licList.get(d).equals(licFromFile[g]))
	                				 break;
	                			 else
	                				 licList.add(licFromFile[g]);
	                		 }
	                	 }
	                	 AnyLicenseInfo[] mergedLicFromFile = licList.toArray(new AnyLicenseInfo[licList.size()]);
	                	 masterPackage.setLicenseInfosFromFiles(mergedLicFromFile);
	                	 String[] skippedFiles = collectSkippedFiles(masterPackage,tempPackage);
	                	 VerificationCodeGenerator verCodeGenerator = new VerificationCodeGenerator(new JavaSha1ChecksumGenerator());
	                	 SpdxPackageVerificationCode newCode = verCodeGenerator.generatePackageVerificationCode(fileMergeResult, skippedFiles);
	                	 masterPackage.setPackageVerificationCode(newCode);
	                	 retval.set(index, masterPackage);
	                }
	                
				}
				
			}
			return packagesResult;
		}
        
	    /** A method to check license from file information through mapLicenseInfo function. If it is a
	     * non-standard license, check this particular license in the Mapper class. Replace the current 
	     * license value with the return value from the mapNonStdLicInMap method. After all, all the declared
	     * license should remain the same and non-standard license should replaced with updated id
	     * @param doc
	     * @param temp
	     * @return licFromFile
	     */
		public AnyLicenseInfo[] checkLicenseFromFile(SpdxDocument doc, SpdxPackage temp){
			AnyLicenseInfo[] licFromFile = temp.getLicenseInfoFromFiles();
			for(int k = 0; k < licFromFile.length; k++){
				if(licFromFile[k] instanceof ExtractedLicenseInfo){
					AnyLicenseInfo tempLicense = mapper.mapNonStdLicInMap(doc, licFromFile[k]);
					licFromFile[k] = tempLicense;
				}
			}			
			return licFromFile;
		}
		
		/** A method to clone the packages in the list 
		 * @param packagesArray
		 * @return clonedPackagesArray
		 */
		public List<SpdxPackage> clonePackages(List<SpdxPackage> packagesList){
			List<SpdxPackage> clonedPackagesList = new ArrayList<SpdxPackage>();
			for(int h = 0; h < packagesList.size(); h++){
				clonedPackagesList.add(packagesList.get(h).clone());
			}
			return clonedPackagesList;
		}
		
		@Deprecated
		/**
		 * 
		 * @param subDocs
		 * @param fileMergeResult
		 * @return
		 * @throws InvalidSPDXAnalysisException
		 * @throws NoSuchAlgorithmException
		 * @throws InvalidLicenseStringException
		 */
		public SpdxPackage mergePackageInfo(SpdxDocument[] subDocs, SpdxFile[] fileMergeResult) 
				throws InvalidSPDXAnalysisException, NoSuchAlgorithmException, InvalidLicenseStringException{
			
			AnyLicenseInfo declaredLicense = LicenseInfoFactory.parseSPDXLicenseString("NOASSERTION");
			packageInfoResult.setLicenseDeclared(declaredLicense);		
			
			String licComments = translateSubDelcaredLicsIntoComments(subDocs);
			packageInfoResult.setLicenseComments(licComments);
						
			return packageInfoResult;			
		}
		
		/**
		 * A method to collect all skipped files from input SPDX package.
		 * @return excludedFileNamesArray
		 * @throws InvalidSPDXAnalysisException
		 */
		public String[] collectSkippedFiles(SpdxPackage main, SpdxPackage sub ) throws InvalidSPDXAnalysisException{
			ArrayList<String> excludedFileNamesList = new ArrayList<String>();
			String[] retval = sub.getPackageVerificationCode().getExcludedFileNames();
			String[] skippedFileInMain = main.getPackageVerificationCode().getExcludedFileNames();
			
			if(skippedFileInMain.length == 0 && retval.length == 0){
				String[] excludedFileNamesArray = new String[0];
				return excludedFileNamesArray;
			}
			else if(skippedFileInMain.length != 0){
				excludedFileNamesList = new ArrayList<String>(Arrays.asList(skippedFileInMain));
			}
			else if(excludedFileNamesList.isEmpty() && retval.length != 0){
				excludedFileNamesList = new ArrayList<String>(Arrays.asList(retval));
			}
			else{
					for(int k = 0; k < retval.length; k++){
						boolean foundNameMatch = false;
						for(int q = 0; q < excludedFileNamesList.size(); q++){
							if(retval[k].equalsIgnoreCase(excludedFileNamesList.get(q))){
								foundNameMatch = true;
							}
						}
						if(!foundNameMatch){
							excludedFileNamesList.add(retval[k]);
						}
					}
				}
			String[] excludedFileNamesArray = new String[excludedFileNamesList.size()];
			excludedFileNamesList.toArray(excludedFileNamesArray);
			excludedFileNamesList.clear();
			return excludedFileNamesArray;
		}

		@Deprecated
		/**
		 * 
		 * @param subDocs
		 * @return
		 * @throws InvalidSPDXAnalysisException
		 */
		public String translateSubDelcaredLicsIntoComments(SpdxDocument[] subDocs) throws InvalidSPDXAnalysisException{
			SpdxLicenseMapper mapper = new SpdxLicenseMapper();
				if(!mapper.isNonStdLicIdMapEmpty()){
					StringBuilder buffer = new StringBuilder(packageInfoResult.getLicenseComments() 
							+ ". This package merged several packages and the sub-package contain the following licenses:");

					for(int k = 0; k < subDocs.length; k++){
						if(mapper.docInNonStdLicIdMap(subDocs[k])){
						 List<SpdxPackage> tempList = subDocs[k].getDocumentContainer().findAllPackages();
						  for(int h = 0; h < tempList.size(); h++){
							AnyLicenseInfo license = tempList.get(h).getLicenseDeclared();
							AnyLicenseInfo result = mapper.mapLicenseInfo(subDocs[k], license); 
							buffer.append(tempList.get(h).getPackageFileName());
							buffer.append(" (" + result.toString() + ") ");
						  }
						}else{
							List<SpdxPackage> tempList = subDocs[k].getDocumentContainer().findAllPackages();
							for(int q = 0; q < tempList.size(); q++){
							buffer.append(tempList.get(q).getPackageFileName());
							buffer.append(" (" + tempList.get(q).getLicenseDeclared().toString() + ") ");
						  }
						}
					}			
					return buffer.toString();
			}else{
				return packageInfoResult.getLicenseComments();
			}
		}
}
