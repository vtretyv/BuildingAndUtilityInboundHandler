/**
     * Email services are automated processes that use Apex classes
     * to process the contents, headers, and attachments of inbound
     * email.
     */
     /**
     *  @Description            :   This is a handler for Utility Money Out and Buildings for Utility Money out inserts.
     *
     *  @Created By             :   Vlad
     *
     *  @Created Date           :   3/22/2018
     *
     *  @Version                :   V_1.0 
     *
     *  @Revision Log           :   V_1.0 - Created  
     **/
    global class BuildingsAndUtilityInsertInboundHandler implements Messaging.InboundEmailHandler {
        
        //Variables 
        // global static List<Opportunity> properties;
        //Arrays that will store utilites and buildings once batch code is implemented
        // global static List<UtiltyMoneyOut__c> utilities;
        // global static List<BuildingForUMO__c> buildings;
        global static String errorMessage = '';
        // global static Map<String,String> mapOfExceptions;
        //Constructor
        global BuildingsAndUtilityInsertInboundHandler() {
            //utilities = new List<UtilityMoneyOut__c>();
            //buildings = new List<BuildingForUMO__c>();
            // mapOfExceptions = new Map<String,String>();
        }
        /**
         *  @purpose    :   This method is written in Messaging.InboundEmailHandler insertface which invoke the email service.
         *
         *  @return     :   Messaging.InboundEmailResult
         *
         *  @args       :   Messaging.InboundEmail email, Messaging.InboundEnvelope envelope
        **/
        global Messaging.InboundEmailResult handleInboundEmail(Messaging.InboundEmail email, Messaging.InboundEnvelope envelope) {
            
            //Getting email result to return
            Messaging.InboundEmailResult result = new Messaging.InboundEmailresult();
            
            //List of binary Attachments attached wiyh Email
            List<Messaging.InboundEmail.BinaryAttachment> bAttachments = new List<Messaging.InboundEmail.BinaryAttachment>();
            
            
            //Code added for address matching
            // List<Opportunity> listOfPropertyInserted = new List<Opportunity>();
            // List<Opportunity> listOfPropertyToBeUpdateCode = new List<Opportunity>();
            // List<Opportunity> listOfPropertyTobeUpdateAdd = new List<Opportunity>();
           
            //fetching attachments
            bAttachments = email.binaryAttachments;
            
            //String to hold File  name
            String fileName =  '';
            
            //String to hold Email subject
            String subject = email.Subject;
            
            List<String> strList = new List<String>();
            if(subject.contains('from')) {
                //List of string to split subject using 'from'
                strList = subject.split('from');
            }
            
            //String to hold email address from subject of mail
            if(strList.size() > 0)
                subject = strList[strList.size() - 1];
            //Check to see what the subject is and then use one of 3 inbound handlers 
            if (subject ==  'Utilities Money Out') {
                if(bAttachments != null) {
                    
                    //Loop over list of attachments
                    for(Messaging.InboundEmail.BinaryAttachment bAttach : bAttachments) {
                        
                        //Blob instance of attachment file body
                        Blob body = bAttach.body;
                        
                        //complete content of file into a string
                        String bodyOfFile = body.toString();
                        
                        //Check if attachment body is null
                        if(bodyOfFile != null) {
                            
                            //method call for read csv file one by one line
                            readCsvFileUtilitiesMoneyOut(bodyOfFile, bAttach.fileName);
                            
                            //Assign file name
                            fileName = bAttach.fileName;
                        }
                    }
                }
                return result;
            }
            else if (subject == 'Buildings for Utilities Money Out') { 
                if(bAttachments != null) {
                    
                    //Loop over list of attachments
                    for(Messaging.InboundEmail.BinaryAttachment bAttach : bAttachments) {
                        
                        //Blob instance of attachment file body
                        Blob body = bAttach.body;
                        
                        //complete content of file into a string
                        String bodyOfFile = body.toString();
                        
                        //Check if attachment body is null
                        if(bodyOfFile != null) {
                            
                            //method call for read csv file one by one line
                            readCsvBuildingsUMO(bodyOfFile, bAttach.fileName);
                            
                            //Assign file name
                            fileName = bAttach.fileName;
                        }
                    }
                }
            
                return result;
            }
            else {
                return result;
            }
        }    
        
        //Next Read and Prase for UMO
        //Read UMO here
        global static void readCsvFileUtilitiesMoneyOut(String fileContent, String fileName) {
    
            String errordebug = '';
            try {
                
                String profileId = [select id from Profile where Name ='Lead Agent Profile'].id;
                String AgentId = [select id,Name from user where profileId=:profileId order by CreatedDate ASC limit 1].id;
                
                
                //map to hold custom setting label and Api name
                Map<String,Property_Configuration_Manager__c> mapOfCustomSettingValues = new Map<String,Property_Configuration_Manager__c>();
                
                for(Property_Configuration_Manager__c cSetting : Property_Configuration_Manager__c.getAll().values()) {
                    
                    mapOfCustomSettingValues.put(cSetting.Field_Label__c, cSetting);
                }
                
                
                //parseCSV(fileContent,false);
                
                List<List<String>> parsedCsv = parseUtiliesMoneyOutCSV(fileContent,false);
                
                //Initialization
                //List<String> lineOfFile = new List<String>();
                
                //Spliting file in to list of string
                //lineOfFile = fileContent.split('\r');
                
                //List of header
                List<String> headers = parsedCsv[0];
                System.debug('HeaderSize'+headers.size());
                System.debug(headers);
                // DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
                // DateFormat dtf = new SimpleDateFormat("MM/dd/yyyy HH:mm a");
                //  if(headers.size() <= mapOfCustomSettingValues.size()) {
                errordebug += '%%%%%';
                //Loop to fetch line values
                for(Integer i = 1; i < parsedCsv.size() ; i++) {
                    
                    //List to hold colounm values
                    List<String> strList = parsedCsv[i];
                    
                    // Opportunity opp = new Opportunity(); Commented this out as we're creating utility money out objects not property objects
                    UtilityMoneyOut__c utilityMoneyOut = new UtilityMoneyOut__c();
                    //  if(strList.size() == headers.size()) {
                    for(Integer j=0; j < headers.size(); j++) {
                        if (j == 0) {
                            utilityMoneyOut.put('billNumber', Integer.valueOf(parsedCsv[i][j]));
                        }    
                        else if (j == 1) {
                            String[] dateHolder = parsedCsv[i][j].split('/');
                            String myMonth = dateHolder[0];
                            String myDay = dateHolder[1];
                            String myYear = dateHolder[2];
                            Date originalDate = Date.newInstance(Integer.valueOf(myYear),Integer.valueOf(myMonth),Integer.valueOf(myDay));
                            utilityMoneyOut.put('date' , originalDate);
                        }
                        else if (j == 2) {
                            String[] dateHolder = parsedCsv[i][j].split('/');
                            String myMonth = dateHolder[0];
                            String myDay = dateHolder[1];
                            String myYear = dateHolder[2];
                            Date dueDate = Date.newInstance(Integer.valueOf(myYear),Integer.valueOf(myMonth),Integer.valueOf(myDay));
                            utilityMoneyOut.put('dueDate', dueDate);
                        }  
                        else if (j == 3) {
                            utilityMoneyOut.put('ageFromBillDate',Integer.valueOf(parsedCsv[i][j]));
                        }  
                        else if (j == 4) {
                            utilityMoneyOut.put('ageFromDueDate',Integer.valueOf(parsedCsv[i][j]));
                        }  
                        else if (j == 5) {
                            utilityMoneyOut.put('companyName', parsedCsv[i][j]);
                        }  
                        else if (j == 6) {
                            utilityMoneyOut.put('totalBillAmount' ,Decimal.valueOf(parsedCsv[i][j]));
                        }  
                        else if (j == 7) {
                            utilityMoneyOut.put('lastModifiedBy ',parsedCsv[i][j]);
                        }  
                        else if (j == 8) {
                            String[] parts = parsedCsv[i][j].split(' ');
                            String[] dateHolder = parts[0].split('/'); 
                            String[] timeHolder = parts[1].split(':');                          
                            String myMonth = dateHolder[0];
                            String myDay = dateHolder[1];
                            String myYear = dateHolder[2];
                            String myHour = timeHolder[0];
                            String myMin = timeHolder[1];                            
                            Datetime modDate = Datetime.newInstance(Integer.valueOf(myYear),Integer.valueOf(myMonth),Integer.valueOf(myDay),Integer.valueOf(myHour),Integer.valueOf(myMin),00);
                            utilityMoneyOut.put('lastModified' , modDate); //Need to change to Date Time
                        }  
                        else if (j == 9) {
                            utilityMoneyOut.put('account' ,parsedCsv[i][j]);
                        }   
                        else if (j == 10) {
                            //Need to rework payment status into a boolean as it is the ideal data type for it, True = paid, false = unpaid.
                            //Since unpaid contains paid, check for Unpaid first, then paid. This will make sure to log those without a payment status as false.
                            if (parsedCsv[i][j].contains('Unpaid')) {
                            utilityMoneyOut.put('paymentStatus',false);
                            }
                            else if (parsedCsv[i][j].contains('Paid')) {
                            utilityMoneyOut.put('paymentStatus',true);
                            }
                        } 
                        else if (j == 11) {
                            utilityMoneyOut.put('amount' ,Decimal.valueOf(parsedCsv[i][j]));
                        } 
                        else if (j == 12) {
                            utilityMoneyOut.put('amountPaid' ,Decimal.valueOf(parsedCsv[i][j]));
                        } 
                        else if (j == 13) {
                            utilityMoneyOut.put('amountDue' ,Decimal.valueOf(parsedCsv[i][j]));
                        } 
                        else if (j == 14) {
                            utilityMoneyOut.put('portfolio' ,parsedCsv[i][j]);
                        } 
                        else if (j == 15) {
                            utilityMoneyOut.put('building' ,parsedCsv[i][j]);
                        } 
                        else if (j == 16) {
                            utilityMoneyOut.put('unit' ,parsedCsv[i][j]);
                        } 
                        // utilities.add(utilityMoneyOut);
                        insert utilityMoneyOut; 
                    }   
                }
            }
            catch(Exception e) {
                System.debug('Exception');
            }
        }
        
        //Parse UMO here
        global static List<List<String>> parseUtiliesMoneyOutCSV(String contents,Boolean skipHeaders) {
            
            List<List<String>> allFields = new List<List<String>>();
        
            // replace instances where a double quote begins a field containing a comma
            // in this case you get a double quote followed by a doubled double quote
            // do this for beginning and end of a field
                        // contents = contents.replaceAll(',"""',',"DBLQT').replaceall('""",','DBLQT",');
            // now replace all remaining double quotes - we do this so that we can reconstruct
            // fields with commas inside assuming they begin and end with a double quote
                        //contents = contents.replaceAll('""','DBLQT');
            // we are not attempting to handle fields with a newline inside of them
            // so, split on newline to get the spreadsheet rows
            List<String> lines = new List<String>();
            List<String> lns = new List<String>();
            try {
                lines = contents.replace('$','').split('\n');
                List<Integer> indexes = new List<Integer>();
                
                String strRows = lines[0];
                //Changed PARSE CSV to fit utilities money out. This will now drop all rows before the header row.
                while(!(strRows.trim().contains('Bill #'))) {                
                // while(!(strRows.trim().contains('Building ID Number'))) {
                    lines.remove(0);
                    strRows = lines[0];
                }
                //The following code trims off the last line and has remained untouched
                strRows = lines[lines.size()-1];
                if(strRows.trim().contains('Generated By')) {
                    lines.remove(lines.size()-1);
                }
                
                Integer i = 0;
               for(String str: lines) {
                    
                    if((String.isBlank(str.remove('"').remove(',')))) {
                        
                        indexes.add(i);   
                    }
                    else {
                        lns.add(str);
                    }
                    i++;
                }
                
                
                System.debug('lns' + lns.size());
                system.debug('lines'+lines.size());
             
            } catch (System.ListException e) {
               System.debug('Limits exceeded?' + e.getMessage());
            }
            String deb = '';
            Integer num = 0;
            for(String line : lines) {
                // check for blank CSV lines (only commas)
                if (line.replaceAll(',','').trim().length() == 0) break;
                
                 line.removeEnd(',');
                 system.debug('line'+line);
                 String line2 = line.escapeCsv();
                 //system.debug('line2'+line2);
                
                List<String> fields = line.split(',');
               
                List<String> cleanFields = new List<String>();
                String compositeField = '';
                Boolean makeCompositeField = false;
                
                for(String field : fields) {
                    deb += field+'&&&&';
                     //Check if string is null
                    if(!String.IsBlank(field)) {
                
                        if ((field.trim()).startsWith('"') && (field.trim()).endsWith('"') && (field.trim() != '"')) {
                            cleanFields.add(field.replaceAll('DBLQT','"').trim());
                            deb += 'First';
                        } else if (makeCompositeField == false &&field.trim().startsWith('"')) {
                            makeCompositeField = true;
                            compositeField = field.trim();
                            deb += 'Second';
                        } else if (makeCompositeField == true && field.trim().endsWith('"')) {
                            compositeField += ',' + field;
                            cleanFields.add(compositeField.replaceAll('DBLQT','"').trim());
                            makeCompositeField = false;
                            compositeField = '';
                            deb += 'third';
                        } else if (makeCompositeField) {
                            
                            compositeField +=  ',' + field.trim();
                            deb += 'fourth';
                        } 
                        else {
                            cleanFields.add(field.replaceAll('DBLQT','"').trim());
                            deb += 'fifth';
                        }
                    }
                     else {
                        //Set as blank
                         cleanFields.add('');
                    }
                    
                }
               
                allFields.add(cleanFields);
            }
            
           system.debug('&&&&&&&&&&&&&&&&&&&&deb'+deb);
            if (skipHeaders)
             allFields.remove(0);
            return allFields;  
                 
    }
    
    //Read and parse for Buildings for UMO
    //Read First
    global static void readCsvBuildingsUMO(String fileContent, String fileName) {
    
        String errordebug = '';
        try {
            
            //map to hold custom setting label and Api name
            // Map<String,Property_Configuration_Manager__c> mapOfCustomSettingValues = new Map<String,Property_Configuration_Manager__c>();
            
            // for(Property_Configuration_Manager__c cSetting : Property_Configuration_Manager__c.getAll().values()) {
                
            //     mapOfCustomSettingValues.put(cSetting.Field_Label__c, cSetting);
            // }
            
            
            //parseCSV(fileContent,false);
            
            List<List<String>> parsedCsv = parseBuildingsForUtiliesMoneyOutCSV(fileContent,false);
            
            //Initialization
            //List<String> lineOfFile = new List<String>();
            
            //Spliting file in to list of string
            //lineOfFile = fileContent.split('\r');
            
            //List of header
            List<String> headers = parsedCsv[0];
            System.debug('HeaderSize'+headers.size());
            System.debug(headers);
            // DateFormat df = new SimpleDateFormat("MM/dd/yyyy"); 
            //  if(headers.size() <= mapOfCustomSettingValues.size()) {
            errordebug += '%%%%%';
            //Loop to fetch line values
            for(Integer i = 1; i < parsedCsv.size() ; i++) {
                
                //List to hold colounm values
                List<String> strList = parsedCsv[i];
                
                // Opportunity opp = new Opportunity(); Commented this out as we're creating utility money out objects not property objects
                BuildingForUMO__c building = new BuildingForUMO__c();
                //  if(strList.size() == headers.size()) {
                for(Integer j=0; j < headers.size(); j++) {
                    if (j == 0) {
                        building.put('buildingName' ,parsedCsv[i][j]); //String
                    }    
                    else if (j == 1) {
                        building.put('address' ,parsedCsv[i][j]); //String
                    }
                    else if (j == 2) {
                        if (parsedCsv[i][j].contains('vacant')) {
                            building.put('occupancyStatus' , false); //Bool
                        }
                        else {
                            building.put('occupancyStatus' , true); //Bool
                        }
                    }  
                    else if (j == 3) {
                        building.put('buildingType' ,parsedCsv[i][j]); //String
                    }  
                    else if (j == 4) {
                        building.put('primaryPropertyManager' ,parsedCsv[i][j]); //String
                    }  
                    else if (j == 5) {
                        building.put('targetRent' ,Decimal.valueOf(parsedCsv[i][j])); //Decimal
                    }  
                    else if (j == 6) {
                        building.put('propertyOwnerPrimaryAddress1' ,parsedCsv[i][j]); //String
                    }  
                    else if (j == 7) {
                        building.put('propertyOwnerPrimaryAddress2' ,parsedCsv[i][j]); //String
                    }  
                    else if (j == 8) {
                        building.put('propertyOwners' ,parsedCsv[i][j]); //String
                    }  
                    else if (j == 9) {
                        building.put('allPropertyManagers' ,parsedCsv[i][j]); //String
                    }   
                    else if (j == 10) {
                        building.put('propertyOwnerEmails' ,parsedCsv[i][j]); //String
                    } 
                    else if (j == 11) {
                        building.put('propertyOwnerHomePhones' ,parsedCsv[i][j]); //String
                    } 
                    else if (j == 12) {
                        building.put('propertyOwnerMobilePhones' ,parsedCsv[i][j]); //String
                    } 
                    else {
                        // System.out.print('Uh-Oh, exceeded boundary parameters for Utilities Money Out fields, check the parsing method'); 
                        errordebug+= '*BuildingUMO read Overflow*';
                    }
                    // BuildingsForUtilities.add(Building);   
                    insert building;   
                }   
            }
        }
        catch(Exception e) {
            System.debug('Exception');
        }
    }

    //Parse Buildings for UMO
    global static List<List<String>> parseBuildingsForUtiliesMoneyOutCSV(String contents,Boolean skipHeaders) {
            
        List<List<String>> allFields = new List<List<String>>();
    
        // replace instances where a double quote begins a field containing a comma
        // in this case you get a double quote followed by a doubled double quote
        // do this for beginning and end of a field
                    // contents = contents.replaceAll(',"""',',"DBLQT').replaceall('""",','DBLQT",');
        // now replace all remaining double quotes - we do this so that we can reconstruct
        // fields with commas inside assuming they begin and end with a double quote
                    //contents = contents.replaceAll('""','DBLQT');
        // we are not attempting to handle fields with a newline inside of them
        // so, split on newline to get the spreadsheet rows
        List<String> lines = new List<String>();
        List<String> lns = new List<String>();
        try {
            lines = contents.replace('$','').split('\n');
            List<Integer> indexes = new List<Integer>();
            
            String strRows = lines[0];
            //Changed PARSE CSV to fit utilities money out. This will now drop all rows before the header row.
            while(!(strRows.trim().contains('Building Name'))) {                
            // while(!(strRows.trim().contains('Building ID Number'))) {
                lines.remove(0);
                strRows = lines[0];
            }
            //The following code trims off the last line and has remained untouched
            strRows = lines[lines.size()-1];
            if(strRows.trim().contains('Generated By')) {
                lines.remove(lines.size()-1);
            }
            
            Integer i = 0;
           for(String str: lines) {
                
                if((String.isBlank(str.remove('"').remove(',')))) {
                    
                    indexes.add(i);   
                }
                else {
                    lns.add(str);
                }
                i++;
            }
            
            
            System.debug('lns' + lns.size());
            system.debug('lines'+lines.size());
         
        } catch (System.ListException e) {
           System.debug('Limits exceeded?' + e.getMessage());
        }
        String deb = '';
        Integer num = 0;
        for(String line : lines) {
            // check for blank CSV lines (only commas)
            if (line.replaceAll(',','').trim().length() == 0) break;
            
             line.removeEnd(',');
             system.debug('line'+line);
             String line2 = line.escapeCsv();
             //system.debug('line2'+line2);
            
            List<String> fields = line.split(',');
           
            List<String> cleanFields = new List<String>();
            String compositeField = '';
            Boolean makeCompositeField = false;
            
            for(String field : fields) {
                deb += field+'&&&&';
                 //Check if string is null
                if(!String.IsBlank(field)) {
            
                    if ((field.trim()).startsWith('"') && (field.trim()).endsWith('"') && (field.trim() != '"')) {
                        cleanFields.add(field.replaceAll('DBLQT','"').trim());
                        deb += 'First';
                    } else if (makeCompositeField == false &&field.trim().startsWith('"')) {
                        makeCompositeField = true;
                        compositeField = field.trim();
                        deb += 'Second';
                    } else if (makeCompositeField == true && field.trim().endsWith('"')) {
                        compositeField += ',' + field;
                        cleanFields.add(compositeField.replaceAll('DBLQT','"').trim());
                        makeCompositeField = false;
                        compositeField = '';
                        deb += 'third';
                    } else if (makeCompositeField) {
                        
                        compositeField +=  ',' + field.trim();
                        deb += 'fourth';
                    } 
                    else {
                        cleanFields.add(field.replaceAll('DBLQT','"').trim());
                        deb += 'fifth';
                    }
                }
                 else {
                    //Set as blank
                     cleanFields.add('');
                }
                
            }
           
            allFields.add(cleanFields);
        }
        
       system.debug('&&&&&&&&&&&&&&&&&&&&deb'+deb);
        if (skipHeaders)
         allFields.remove(0);
        return allFields;  
             
    }
    
    
    global static void billingIncreaseAlert() {
        // Calendar cal = Calendar.getInstance();
        // cal.add(Calendar.MONTH, -1);
        Date curDate = Date.today();
        Date fortnitePriorDate = curDate.addDays(-14);
        //Need to use more precise SOQL command instead of SQL << LAST COMMIT  
        // List<UtilityMoneyOut> currentUtils = [SELECT buildingAddress FROM UtilityMoneyOut WHERE building IN (SELECT T1.building FROM UtilityMoneyOut T1 INNER JOIN UtilityMoneyOut T2 ON T1.building = T2.buildingWHERE T1.date > T2.date AND T1.date > fortnitePriorDate AND t2.date > fortnitePriorDate AND T1.amount > T2.amount *(1.5));)];
        // if (currentUtils.length > 0) {
        //     List<string> buildingNames;
        //     String resultString;
            // for (UtiltyMoneyOut utility : currentUtils) {
            //     buildingNames.add(utility.buildingAddress)
            // }
            // List<BuildingForUMO> targetBuildings;
            // for (String name :buildingNames) {
            //     BuildingForUMO curBuilding= [SELECT occupancyStatus, propertyOwners, propertyOwnerEmails, propertyOwnerHomePhones, propertyOwnerMobilePhones FROM BuildingForUMO T1
            //     WHERE T1.buildingName = name;
            //     ]
            //     targetBuildings.add(curBuilding);
            //     //NEED TO FIND A WAY TO GET THE % INCREASE
            //     resultString += curBuilding.occupancyStatus + '|' + curBuilding.propertyOwners + '|' + curBuilding.propertyOwnerEmails + '|' + curBuilding.propertyOwnerHomePhones + '|' + curBuilding.propertyOwnerMobilePhones + '\n';
            // }
            //NEED TO INCLUDE CODE TO SEND EMAIL HERE
            //Create single email object
            Messaging.SingleEmailMessage mail = new Messaging.SingleEmailMessage(); 
            //Set up to emails
            String[] toAddresses = new String[] {'vladimirtretyakov226@gmail.com'};
            //Assign to emails
            mail.setToAddresses(toAddresses);
            //Set recipient reply
            mail.setReplyTo('vladimirtretyakov226@gmail.com');
            //Set email display name
            mail.setSenderDisplayName('Vlad Debug');
            //Set Sbject
            mail.setSubject('50% alert Debug');
            //No bcc
            mail.setBccSender(false);
            //Include plain body result
            // mail.setPlainTextBody('Here is the resuling string : ',  +resultString);
            //Set HTML body
            // mail.setHtmlBody('Here is the resuling string : ',  +resultString);
            //Send the email
            Messaging.sendEmail(new Messaging.SingleEmailMessage[] {mail });
        // }
        // else {
        //     Messaging.SingleEmailMessage mail = new Messaging.SingleEmailMessage(); 
        //     //Set up to emails
        //     String[] toAddresses = new String[] {'vladimirtretyakov226@gmail.com'};
        //     //Assign to emails
        //     mail.setToAddresses(toAddresses);
        //     //Set recipient reply
        //     mail.setReplyTo('vladimirtretyakov226@gmail.com');
        //     //Set email display name
        //     mail.setSenderDisplayName('Vlad Debug');
        //     //Set Sbject
        //     mail.setSubject('No Utility Bill Increase');
        //     //No bcc
        //     mail.setBccSender(false);
        //     //Send the email
        //     Messaging.sendEmail(new Messaging.SingleEmailMessage[] {mail });
        // }
    }
        
}