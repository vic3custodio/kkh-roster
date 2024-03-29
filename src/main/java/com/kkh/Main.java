package com.kkh;

import java.io.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.*;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.google.api.services.sheets.v4.model.Color;

import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;

public class Main {

/*
//List of Rules
//====================================================================================
//1. C cannot appear on filled squares
//2. Populate from highest number of leaves
//3. C - - C, must have at least 2 empty slots in between
//4. Each column must have 3Cs, else notify Jason (including existing C or CR) (notify by changing date cells fill colour to red)
//5. Each Row Total number of C <= 6 (including existing C or CR) (found in column AH: for Jan)(i.e. not always column AH for every month) (or else notify jason by changing date cell fill colour to red of those days without 3 C)
//6. Dark squares cant have C
//7. Only 1 blue dr on C everyday, else to notify which dates unable to fulfill (notify by changing date cell fill colour to red)
//8. Rows for white Drs and Dr Zainab and Dr Nandita cannot be populated (if possible to include a selection option of rows/ Drs where C dun populate)
//9. C not to be located before L if possible
//10. Equalise no. of C for all Fri, Sat and Sun between rows
//11. No 2 consecutive Sat C
//====================================================================================
*/

    private static final String APPLICATION_NAME = "kkh-roster";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this kkh-roster.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = new ArrayList<>();//Collections.singletonList(SheetsScopes.DRIVE,SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    public static final String BLUE = "BLUE";
    public static final String WHITE = "WHITE";
    public static final String GREEN = "GREEN";
    public static final String C = "C";
    public static final String L = "L";
    public static final String F = "F";
    public static final String DOT = ".";

    private static Set<String> skip;
    private static Color doNotFillColor;
    private static Color blueDoctorColor;

    static {
        SCOPES.add(SheetsScopes.DRIVE);
        SCOPES.add(SheetsScopes.DRIVE_FILE);
        SCOPES.add(SheetsScopes.SPREADSHEETS);

        skip = new HashSet<>();
        skip.add("Events");
        skip.add("Teaching");
        skip.add("Department Juniors");
        skip.add("Residents");
        skip.add("TOTAL NUMBER ON LEAVE");
        //skip.add("No of calls");

        doNotFillColor = new Color();
        doNotFillColor.setBlue(0.8f);
        doNotFillColor.setGreen(0.8f);
        doNotFillColor.setRed(0.8f);

        blueDoctorColor = new Color();
        blueDoctorColor.setBlue(0.9529412f);
        blueDoctorColor.setGreen(0.8862745f);
        blueDoctorColor.setRed(0.8117647f);
    }

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = Main.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            System.out.println("Warning " + CREDENTIALS_FILE_PATH + " not found!!!");
            in = new FileInputStream("../resources" + CREDENTIALS_FILE_PATH);
            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
            }
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public static void main(String[] args) throws Exception {
        //String dateRange = "2019-01-01to2019-01-15";
        //String dateRange = "2019-01-01";

        //String dateRange = "2019-01-01to2019-01-31";
        //final String sourceSpreadsheetId = "1wIjFxOuctD-f0s9gG3f0QDf2QmkS1tXwT3o5hBU__-0";
        //final String targetSpreadsheetId = "1psqPSDk16rpZBrQ-CP1_NS_-eDf4BLIG95ehdk_auCQ";
        //final String range = "test!A3:AG";

        String dateRange = "2019-07-02to2019-08-04";
        final String sourceSpreadsheetId = "1P3u-LNPo05yMlhYveFEcb4M43wAAMeraOXtN4aUN2kE";
        final String targetSpreadsheetId = "1nXjLp8ZFsNjHqyn8nzPUuu5MVklVNcdnPzcpjyuWPHU";
        final String range = "July!A4:AJ";

        final String target2SpreadsheetId = "1jvCYffTi4Whr6saDuPDZfr8xViB1yxW-HgOxjpt2gRQ";
        final String range2 = "July 2019!A4:AI";

        //final String range = "test!A3:Q";

        String[] appArgs = new String[7];
        if (args == null || args.length < 1) {
            String[] dates = dateRange.split("to");
            appArgs[0] = dates[0];
            if (dates.length > 1) {
                appArgs[1] = dates[1];
            }
            else {
                appArgs[1] = dates[0];
            }
            appArgs[2] = sourceSpreadsheetId;
            appArgs[3] = targetSpreadsheetId;
            appArgs[4] = range;
            appArgs[5] = target2SpreadsheetId;
            appArgs[6] = range2;
        } else {
            System.out.println("Program arguments was provided!!!");
            String[] dates = args[0].split("to");
            appArgs[0] = dates[0];
            if (dates.length > 1) {
                appArgs[1] = dates[1];
            }
            else {
                appArgs[1] = dates[0];
            }
            appArgs[2] = args[1];
            appArgs[3] = args[2];
            if (args.length > 3) {
                appArgs[4] = args[3];
            } else {
                appArgs[4] = range;
            }
            appArgs[5] = args[4];
            appArgs[6] = args[5];
        }

        generateC(appArgs);
    }

    public static void generateC(String[] args) throws Exception {

        // fill the sheet with known information (doctor's name, type, color, days leave, C, filled)
        String dateFrom = args[0];
        String dateTo = args[1];
        //default, ISO_LOCAL_DATE
        LocalDate localDateFrom = LocalDate.parse(dateFrom);
        LocalDate localDateTo = LocalDate.parse(dateTo);
        if (localDateFrom.compareTo(localDateTo) == 0) {
            Month month = localDateFrom.getMonth();
            Integer lastDayOfTheMonth = month.length(localDateFrom.isLeapYear());
            String date = localDateFrom.getYear() + "-" + padWithZero("" + localDateFrom.getMonthValue()) + "-" + padWithZero("" + lastDayOfTheMonth);
            //default, ISO_LOCAL_DATE
            localDateTo = LocalDate.parse(date);
        }

        Long daysBetween = DAYS.between(localDateFrom, localDateTo) + 1;

        // scan the sheet once to take the inputted information.
        List<Doctor> doctors = new ArrayList<>();

        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        final String sourceSpreadsheetId = args[2];

        final String range = args[4];
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        Map<LocalDate, Day> dayMap = new HashMap<>();

        Sheets.Spreadsheets.Get get = service.spreadsheets().get(sourceSpreadsheetId).setIncludeGridData(true);
        Spreadsheet spreadsheet = get.execute();
        List<GridData> gridDatas = spreadsheet.getSheets().get(0).getData();

        System.out.printf("GridData size : %s\n", gridDatas.size());
        // Value : F, Cell color : {"blue":0.8,"green":0.8,"red":0.8}
        // Value : Lye Siyu (R2) [1/10-2/1/19], Cell color : {"blue":0.9529412,"green":0.8862745,"red":0.8117647}

        for (GridData gridData: gridDatas) {
            int ctr = 1;
            System.out.printf("RowData size : %s\n", gridData.getRowData().size());
            for (RowData rowData: gridData.getRowData()) {
                if (rowData.getValues() == null) {
                    System.out.printf("RowData is null\n");
                    continue;
                }
                else if (rowData.getValues().size() <= 2) {
                    System.out.printf("RowData size : %s\n", rowData.getValues().size());
                    continue;
                }
                else if (rowData.getValues().size() > 2) {
                    //System.out.printf("RowData size : %s\n", rowData.getValues().size());
                }
                CellData doctorCellData = rowData.getValues().get(0);
                CellData doctorTierCellData = rowData.getValues().get(1);
                //if (ctr == 1 || ctr == 2 || doctorCellData.getFormattedValue() == null || skip.contains(doctorCellData.getFormattedValue())) {
                //    ctr++;
                //    continue;
                //}
                Doctor doctor = new Doctor(doctorCellData.getFormattedValue());
                if ("R3".equals(doctorTierCellData.getFormattedValue())) {
                    doctor.setColor(BLUE);
                }
                //else if (doctorCellData.getEffectiveFormat() != null && doctorCellData.getEffectiveFormat().getBackgroundColor() != null && doctorCellData.getEffectiveFormat().getBackgroundColor().equals(blueDoctorColor)) {
                //    System.out.printf("Pre-Setting doctor to BLUE Doctor Name: %s\n", doctor.getName());
                //    doctor.setColor(BLUE);
                //}
                else if ("R0".equals(doctorTierCellData.getFormattedValue())) {
                    doctor.setColor(WHITE);
                }
                else if ("R2".equals(doctorTierCellData.getFormattedValue())) {
                    doctor.setColor(GREEN);
                }
                else {
                    continue;
                }

                for (int i=1; i<=daysBetween.intValue(); i++) {
                    CellData cellData = rowData.getValues().get(i+1);
                    Day day = null;
                    LocalDate ld = localDateFrom.plusDays(i-1);
                    if (dayMap.containsKey(ld)) {
                        day = dayMap.get(ld);
                    } else {
                        day = new Day(ld.getDayOfMonth());
                        dayMap.put(ld, day);
                    }
                    if (cellData != null) {
                        //if (cellData != null && cellData.getEffectiveFormat() != null && cellData.getEffectiveFormat().getBackgroundColor() != null) {
                        //    System.out.printf("Value : %s, Cell color : %s\n", cellData.getFormattedValue(), cellData.getEffectiveFormat().getBackgroundColor());
                        //}
                        setCellAndAddToDoctor(doctor, day, cellData, cellData.getFormattedValue(), doNotFillColor);
                    }
                    else {
                        setCellAndAddToDoctor(doctor, day, cellData, "", doNotFillColor);
                    }
                }
                doctors.add(doctor);
            }
            break;
        }

        // scan the sheet second time to generate the C
        // do it 3 times to distribute evenly
        assignCToDoctors(sortAccordingToLeavesDesc(doctors), dayMap, daysBetween.intValue(), localDateFrom, 1, 3);
        assignCToDoctors(sortAccordingToCsAsc(doctors), dayMap, daysBetween.intValue(), localDateFrom, 1, 4);
        assignCToDoctors(sortAccordingToCsAsc(doctors), dayMap, daysBetween.intValue(), localDateFrom, 1, 5);
        assignCToDoctors(sortAccordingToCsAsc(doctors), dayMap, daysBetween.intValue(), localDateFrom, 1, 6);
        assignCToDoctors(sortAccordingToCsAsc(doctors), dayMap, daysBetween.intValue(), localDateFrom, 2, 6);
        assignCToDoctors(sortAccordingToCsAsc(doctors), dayMap, daysBetween.intValue(), localDateFrom, 3, 6);
        assignCToDoctors(sortAccordingToCsAsc(doctors), dayMap, daysBetween.intValue(), localDateFrom, 3, 6);

        // output the content to verify
        List<Object> cTotals = outputToVerify(doctors, dayMap, localDateFrom, daysBetween.intValue());

        // copy the generated data to junior leave sheet
        copyResultToTargetSheet(service, sourceSpreadsheetId, range, args, doctors, daysBetween.intValue(), cTotals, skip);

        // copy the generated data to master monthly roster sheet
        final String target2SpreadsheetId = args[5]; //"1jvCYffTi4Whr6saDuPDZfr8xViB1yxW-HgOxjpt2gRQ";
        final String range2 = args[6]; //"July 2019!A3:AI";
        copyResultToMasterSheet(service, target2SpreadsheetId, range2, args, doctors, daysBetween.intValue(), cTotals, skip);

        // no need to do this
        // copy the generated data to allocation sheet
        //copyResultToAllocationSheet(service, doctors, daysBetween.intValue(), localDateFrom, dayMap);

        //copyWeeklyRosterSheetToDoc();

    }

    private static void copyResultToTargetSheet(Sheets service, String sourceSpreadsheetId, String range, String[] args, List<Doctor> doctors, Integer daysBetween, List<Object> cTotals, Set<String> skip) throws Exception {
        ValueRange response = service.spreadsheets().values()
                .get(sourceSpreadsheetId, range)
                .execute();

        List<List<Object>> values = response.getValues();

        int j = 0;
        List<List<Object>> updatedValues = new ArrayList<>();
        for (List row : values) {

            boolean isSkip = false;
            for (int i = 0; i <= (daysBetween+1); i++) {

                if (row == null) {
                    isSkip = true;
                    break;
                }
                else if (row.size() < 2) {
                    isSkip = true;
                    break;
                }
                if (!isTier(row.get(1))) {
                    isSkip = true;
                    break;
                }


                if (row.size() > i) {
                    if (i == 0) { // first column is the name
                        String name = row.get(i).toString();
                        //if (skip.contains(name)) {
                        //    isSkip = true;
                        //    break;
                        //}
                    }
                    else if (i == 1) { // second column is the tier
                        row.set(i, row.get(i).toString());
                    }
                    else {
                        if (j < doctors.size()) {
                            if (doctors.get(j).getCells().size() >= (i - 2)) {
                                String value = doctors.get(j).getCells().get(i - 2).getValue();
                                if (value != null && !F.equals(value)) {
                                    row.set(i, doctors.get(j).getCells().get(i - 2).getValue());
                                } else {
                                    row.set(i, "");
                                }
                            }
                        }
                    }
                }
                else {
                    if (i == 1) { // second column is the tier
                        row.add("");
                    }
                    else if (j < doctors.size() && i > 1) {
                        String value = doctors.get(j).getCells().get(i - 2).getValue();
                        if (value != null && !F.equals(value)) {
                            row.add(value);
                        }
                        else {
                            row.add("");
                        }
                    }
                }
            }
            if (!isSkip) {
                j++;
            }
            updatedValues.add(row);
        }
        cTotals.add(0, "");
        updatedValues.add(cTotals);

        ValueRange body = new ValueRange().setValues(updatedValues);

        final String targetSpreadsheetId = args[3];

        service.spreadsheets().values().update(targetSpreadsheetId, range, body)
                //.setValueInputOption("USER_ENTERED")
                .setValueInputOption("RAW")
                .execute();
    }

    private static void copyResultToMasterSheet(Sheets service, String sourceSpreadsheetId, String range, String[] args, List<Doctor> doctors, Integer daysBetween, List<Object> cTotals, Set<String> skip) throws Exception {
        Map<String, Doctor> doctorMap = new HashMap<>();
        for (Doctor doctor: doctors) {
            doctorMap.put(doctor.getName(), doctor);
        }

        ValueRange response = service.spreadsheets().values()
                .get(sourceSpreadsheetId, range)
                .execute();

        List<List<Object>> values = response.getValues();

        List<List<Object>> updatedValues = new ArrayList<>();
        for (List row : values) {

            String name = null;
            Doctor doctor = null;
            for (int i = 0; i < (daysBetween+1); i++) {

                if (row == null) {
                    break;
                }
                if (row.size() > 0 && i == 0) { // first column is the name
                    name = row.get(0).toString();
                    if (doctorMap.get(name) != null) {
                        doctor = doctorMap.get(name);
                        continue;
                    }
                    else {
                        break;
                    }
                }

                if (doctor == null) {
                    break;
                }

                if (row.size() > i) {
                    if (i == 0) { // first column is the name
                    }
                    else {
                        String value = doctor.getCells().get(i-1).getValue();
                        if (value != null && !F.equals(value)) {
                            row.set(i, value);
                        } else {
                            row.set(i, "");
                        }
                    }
                }
                else {
                    String value = doctor.getCells().get(i-1).getValue();
                    if (value != null && !F.equals(value)) {
                        row.add(value);
                    }
                    else {
                        row.add("");
                    }
                }
            }
            updatedValues.add(row);
        }

        ValueRange body = new ValueRange().setValues(updatedValues);

        service.spreadsheets().values().update(sourceSpreadsheetId, range, body)
                //.setValueInputOption("USER_ENTERED")
                .setValueInputOption("RAW")
                .execute();
    }

    private static void copyResultToAllocationSheet(Sheets service, List<Doctor> doctors, Integer daysBetween, LocalDate localDateFrom, Map<LocalDate, Day> dayMap) throws Exception {
        Map<String, Doctor> doctorMap = new HashMap<>();
        for (Doctor doctor: doctors) {
            doctorMap.put(doctor.getName(), doctor);
        }

        final String target3SpreadsheetId = "1k7TTtk8Rxg6G39qqkaaAwLl3--kXpq8od4i2-frKBq8";
        final String range3 = "allocate!A4:B";
        final String range4 = "allocate!GW4:HJ";

        //GetSpreadsheetByDataFilterRequest r = new GetSpreadsheetByDataFilterRequest();
        ValueRange response = service.spreadsheets()
                //.getByDataFilter("", r)
                .values()
                .get(target3SpreadsheetId, range3)
                //.getByDataFilter("", r)
                .execute()
                ;

        ValueRange response2 = service.spreadsheets()
                //.getByDataFilter("", r)
                .values()
                .get(target3SpreadsheetId, range4)
                //.getByDataFilter("", r)
                .execute()
                ;

        List<List<Object>> values = response.getValues();

        List<Integer> indexesToUpdate = new ArrayList<>();
        List<String> doctorsSet = new ArrayList<>();
        int index = 0;
        for (List row: values) {

            //String name = null;
            if (row.size() > 0 ) { // first column is the name
                String name = row.get(0).toString();
                if (doctorMap.get(name) != null) {
                    if (!doctorsSet.contains(name)) {
                        System.out.println("");
                        System.out.printf("index : %s, Doctor : %s", index, name);
                        indexesToUpdate.add(index);
                        doctorsSet.add(name);
                    }
                }
            }
            index++;
        }

        List<List<Object>> values2 = response2.getValues();

        List<List<Object>> updatedValues = new ArrayList<>();
        int index4 = 0;
        int ctr = -1;
        Integer startDate = 22;
        String dateStart = localDateFrom.getYear() + "-" + padWithZero("" + localDateFrom.getMonthValue()) + "-" + padWithZero("" + startDate);
        LocalDate localDateStart = LocalDate.parse(dateStart);

        for (List row: values2) {

            if (!indexesToUpdate.contains(index4)) {
                index4++;
                continue;
            }
            else {
                ctr++;
            }
            //String name = null;
            //Doctor doctor = null;
            int x = 0;
            for (int i = 0; i < (daysBetween); i++) {

                if (row == null) {
                    break;
                }
                /*
                if (row.size() > 0 && i == 0) { // first column is the name
                    name = row.get(0).toString();
                    if (doctorMap.get(name) != null) {
                        doctor = doctorMap.get(name);
                        continue;
                    }
                    else {
                        break;
                    }
                }

                if (doctor == null) {
                    break;
                }
                */

                LocalDate ld = localDateFrom.plusDays(i);
                Day day = dayMap.get(ld);

                if (ld.compareTo(localDateStart) < 0) {
                    continue;
                }

                if (row.size() > x) {
                    //if (i == 0) { // first column is the name
                    //}
                    //else {
                    Doctor doctor = doctorMap.get(doctorsSet.get(ctr));
                    String value = doctor.getCells().get(i).getValue();
                        if (value != null && !F.equals(value)) {
                            System.out.println("");
                            System.out.printf("Doctor : %s, Date : %s, Value : %s", doctor.getName(), ld.toString(), value);
                            row.set(x, value);
                        } else {
                            System.out.println("");
                            System.out.printf("Doctor : %s, Date : %s, Value : %s", doctor.getName(), ld.toString(), "null");
                            row.set(x, "");
                        }
                    //}
                }
                else {
                    if (doctorsSet.size() > ctr) {
                        Doctor doctor = doctorMap.get(doctorsSet.get(ctr));
                        String value = doctor.getCells().get(i).getValue();
                        if (value != null && !F.equals(value)) {
                            System.out.println("");
                            System.out.printf("add Doctor : %s, Date : %s, Value : %s", doctor.getName(), ld.toString(), value);
                            row.add(value);
                        } else {
                            System.out.println("");
                            System.out.printf("add Doctor : %s, Date : %s, Value : %s", doctor.getName(), ld.toString(), "null");
                            row.add("");
                        }
                    }
                }
                //ctr++;

                x++;
            }
            updatedValues.add(row);
            index4++;
        }

        ValueRange body = new ValueRange().setValues(updatedValues);

        service.spreadsheets().values().update(target3SpreadsheetId, range4, body)
                //.setValueInputOption("USER_ENTERED")
                .setValueInputOption("RAW")
                .execute();
    }

    private static void copyWeeklyRosterSheetToDoc() throws Exception {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Docs service = new Docs.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        Document response = service.documents().get("1jNnYxu5M4wQ4IQmobIdq3S5JtaM7xnFn2EUn0EkPhW8").execute();
        String title = response.getTitle();
        System.out.printf("The title of the doc is: %s\n", title);

        Body body = response.getBody();



        List<StructuralElement> contents = body.getContent();
        System.out.println(contents);
        for (StructuralElement content: contents) {
            Table table = content.getTable();
            for (TableRow tableRow: table.getTableRows()) {
                for (TableCell tableCell: tableRow.getTableCells()) {
                    for (StructuralElement cellContent: tableCell.getContent()) {
                        //cellContent.getParagraph().get
                    }
                }
            }
        }



        BatchUpdateDocumentRequest content = new BatchUpdateDocumentRequest();
        com.google.api.services.docs.v1.model.Request request = new com.google.api.services.docs.v1.model.Request();
        //InsertTableRowRequest insertTableRowRequest = new InsertTableRowRequest();
        //TableCellLocation tableCellLocation = new TableCellLocation();
        ////tableCellLocation.setRowIndex()
        //insertTableRowRequest.setTableCellLocation(tableCellLocation);
        ////insertTableRowRequest.set
        //request.setInsertTableRow(insertTableRowRequest);

        InsertTableRequest insertTableRequest = new InsertTableRequest();
        insertTableRequest.setColumns(2);
        insertTableRequest.setRows(2);
        Location location = new Location();
        //location.setIndex();
        //location.setSegmentId();
        insertTableRequest.setLocation(location);
        //insertTableRequest.setEndOfSegmentLocation();
        request.setInsertTable(insertTableRequest);


        List<com.google.api.services.docs.v1.model.Request> requests = new ArrayList<>();
        requests.add(request);
        content.setRequests(requests);
        service.documents().batchUpdate("1jNnYxu5M4wQ4IQmobIdq3S5JtaM7xnFn2EUn0EkPhW8", content)
        ;

    }

    private static List<Object> outputToVerify(List<Doctor> doctors, Map<LocalDate, Day> dayMap, LocalDate localDateFrom, Integer daysBetween) {
        //System.out.println("|Doctor|001|002|003|004|005|006|007|008|009|010|011|012|013|014|015|016|017|018|019|020|021|022|023|024|025|026|027|028|029|030|031|TOT|");
        String header = "|Doctor|";
        for (int i=0; i<daysBetween; i++) {
            LocalDate ld = localDateFrom.plusDays(i);
            Day day = dayMap.get(ld);
            header = header + padTo3Char("" + day.getDay()) + "|";
        }
        System.out.println(header);

        for (Doctor doctor: doctors) {
            String name = doctor.getName().trim();
            if (name.length() >= 6) {
                name = name.substring(0, 6);
            }
            else {
                name = padWithSpace(name, 8);
            }
            String row = "|" + name + "|";
            for (int i=0; i<daysBetween+1; i++) {
                if (i == daysBetween) {
                    row = row + " " + doctor.getCurrentTotalNumberOfCs() + " |";
                }
                else {
                    String cellValue = doctor.getCells().get(i).getValue();
                    row = row + padTo3Char(cellValue) + "|";
                }
            }
            System.out.println(row);
        }
        List<Object> cTotals = new ArrayList<>();
        cTotals.add("");
        String total = "|TOTAL |";
        for (int i=0; i<daysBetween; i++) {
            LocalDate ld = localDateFrom.plusDays(i);
            Day day = dayMap.get(ld);
            total = total + " " + day.getTotalNumberOfDoctorOnC() + " |";
            cTotals.add("" + day.getTotalNumberOfDoctorOnC());
        }
        System.out.println(total);

        return cTotals;
    }

    private static List<Doctor> sortAccordingToLeavesDesc(List<Doctor> doctors) {
        List<Doctor> sortedDoctors = new ArrayList<>(doctors);
        //List<Doctor> sortedDoctors = new ArrayList<>();//(doctors);
        //sortedDoctors.addAll(doctors);
        Collections.sort(sortedDoctors, new Comparator<Doctor>() {
            public int compare(Doctor d1, Doctor d2) {
                return d2.getCurrentTotalNumberOfLs().compareTo(d1.getCurrentTotalNumberOfLs());
            }
        });

        return sortedDoctors;
    }

    private static List<Doctor> sortAccordingToCsAsc(List<Doctor> doctors) {
        List<Doctor> sortedDoctors = new ArrayList<>(doctors);
        Collections.sort(sortedDoctors, new Comparator<Doctor>() {
            public int compare(Doctor d1, Doctor d2) {
                return d1.getCurrentTotalNumberOfCs().compareTo(d2.getCurrentTotalNumberOfCs());
            }
        });

        return sortedDoctors;
    }

    private static List<Doctor> sortAccordingToCsOnWeekendsAsc(List<Doctor> doctors) {
        List<Doctor> sortedDoctors = new ArrayList<>(doctors);
        Collections.sort(sortedDoctors, new Comparator<Doctor>() {
            public int compare(Doctor d1, Doctor d2) {
                return d1.getCurrentTotalNumberOfCs().compareTo(d2.getCurrentTotalNumberOfCs());
            }
        });

        return sortedDoctors;
    }

    private static void assignCToDoctors(List<Doctor> sortedDoctors, Map<LocalDate, Day> dayMap, Integer daysBetween, LocalDate localDateFrom, Integer columnLimit, Integer rowLimit) {
        /*
        List<Doctor> sortedDoctors = new ArrayList<>(doctors);
        Collections.sort(sortedDoctors, new Comparator<Doctor>() {
            public int compare(Doctor d1, Doctor d2) {
                return d2.getCurrentTotalNumberOfLs().compareTo(d1.getCurrentTotalNumberOfLs());
            }
        });
        */

        System.out.printf("First doctor on the list: %s", sortedDoctors.get(0).getName());
        System.out.println("");

        for (int i=0; i<daysBetween; i++) {
            LocalDate ld = localDateFrom.plusDays(i);
            Day day = dayMap.get(ld);
            for (Doctor doctor: sortedDoctors) {
                Cell cell = doctor.getCells().get(i);
                if (cell.getValue() == null || cell.getValue().length() == 0) {
                    //if (!cell.isFilled()) {
                    if (isSpaceBetweenTwoCMoreThanOne(doctor, i, daysBetween)) {
                        if (doctor.getCurrentTotalNumberOfCs() < rowLimit) {
                            if (day.getTotalNumberOfDoctorOnC() < columnLimit) {
                                if ((i < (daysBetween-1) && !doctor.getCells().get(i + 1).isL()) || i == (daysBetween-1)) {
                                    //String date = localDateFrom.getYear() + "-" + padWithZero("" + localDateFrom.getMonthValue()) + "-" + padWithZero("" + (i+1));
                                    //default, ISO_LOCAL_DATE
                                    //LocalDate ld = LocalDate.parse(date);
                                    if (DayOfWeek.SATURDAY.compareTo(ld.getDayOfWeek()) != 0) {
                                        tryToAssignC(doctor, cell, day);
                                    } else {
                                        if (isNotCOnConsecutiveSat(doctor, i, daysBetween)) {
                                            tryToAssignC(doctor, cell, day);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    //}
                }
            }
        }
    }

    private static void setCellAndAddToDoctor(Doctor doctor, Day day, CellData cellData, String cellValue, Color doNotFillColor) {
        Cell cell = new Cell();
        //String cellValue = cellData.getFormattedValue();
        if (cellData.getEffectiveFormat() != null && cellData.getEffectiveFormat().getBackgroundColor() != null && cellData.getEffectiveFormat().getBackgroundColor().equals(doNotFillColor)) {
            cellValue = F;
        }
        setCell(doctor, day, cell, cellValue);
        doctor.getCells().add(cell);
    }

    private static String padWithZero(String month) {
        if (month == null || month.length() == 1) {
            month = "0" + month;
        }
        return month;
    }

    private static String padTo3Char(String str) {
        if (str == null || str.length() == 0) {
            str = "   ";
        }
        else if (str.length() == 2) {
            str = " " + str;
        }
        else {
            str = " " + str + " ";
        }
        return str;
    }

    private static void setCell(Doctor doctor, Day day, Cell cell, String cellValue) {
        if (cellValue == null) {
            cellValue = "";
        }
        if (cellValue.indexOf(C) > -1) {
            System.out.printf("Pre-Setting doctor to %s Doctor Name: %s, Day: %s\n", cellValue, doctor.getName(), day.getDay());
            cell.setValue(cellValue);
            doctor.addCurrentTotalNumberOfCs();
            day.addTotalNumberOfDoctorOnC();
            if (BLUE.equals(doctor.getColor())) {
                day.addNumberOfBlueDoctorOnC();
            }
        }
        else if (cellValue.indexOf(L) > -1) {
            System.out.printf("Pre-Setting doctor to %s Doctor Name: %s, Day: %s\n", cellValue, doctor.getName(), day.getDay());
            cell.setValue(cellValue);
            doctor.addCurrentTotalNumberOfLs();
        }
        else if (cellValue.indexOf(F) > -1) {
            System.out.printf("Pre-Setting doctor to %s Doctor Name: %s, Day: %s\n", cellValue, doctor.getName(), day.getDay());
            cell.setValue(cellValue);
        }
        else if (cellValue.indexOf(DOT) > -1) {
            System.out.printf("Pre-Setting doctor to %s Doctor Name: %s, Day: %s\n", cellValue, doctor.getName(), day.getDay());
            cell.setValue(F);
        }
        else if (WHITE.equals(doctor.getColor())) {
            System.out.printf("Pre-Setting white doctor to %s Doctor Name: %s, Day: %s\n", F, doctor.getName(), day.getDay());
            cell.setValue(F);
        }
        else {
            //System.out.printf("Pre-Setting doctor to blank Doctor Name: %s, Day: %s\n", doctor.getName(), day.getDay());
            cell.setValue("");
        }
    }

    private static void tryToAssignC(Doctor doctor, Cell cell, Day day) {
        if (!BLUE.equalsIgnoreCase(doctor.getColor())) {
            System.out.printf("Setting non-blue doctor to C Doctor Name: %s, Day: %s\n", doctor.getName(), day.getDay());
            cell.setValue(C);
            doctor.addCurrentTotalNumberOfCs();
            day.addTotalNumberOfDoctorOnC();
        } else {
            if (day.getNumberOfBlueDoctorOnC() < 1) {
                System.out.printf("Setting blue doctor to C Doctor Name: %s, Day: %s\n", doctor.getName(), day.getDay());
                cell.setValue(C);
                doctor.addCurrentTotalNumberOfCs();
                day.addTotalNumberOfDoctorOnC();
                day.addNumberOfBlueDoctorOnC();
            }
        }
    }

    private static Boolean isSpaceBetweenTwoCMoreThanOne(Doctor doctor, Integer position, Integer daysOfTheMonth) {
        if ((position < (daysOfTheMonth-1) && doctor.getCells().get(position + 1).isC())
                || (position < (daysOfTheMonth-2) && doctor.getCells().get(position + 2).isC())
                || (position > 0 && doctor.getCells().get(position - 1).isC())
                || (position > 1 && doctor.getCells().get(position - 2).isC())) {
            return false;
        }
        return true;
    }

    private static Boolean isNotCOnConsecutiveSat(Doctor doctor, Integer position, Integer daysOfTheMonth) {
        if ((position < (daysOfTheMonth-7) && doctor.getCells().get(position + 7).isC())
                || (position > 6 && doctor.getCells().get(position - 7).isC())) {
            return false;
        }
        return true;
    }

    private static String padWithSpace(String str, Integer fixedLength) {
        //for (int i=0; i<(fixedLength-str.length()); i++) {
        //    str = str + " ";
        //}
        str = str + "      ";
        return str.substring(0, 6);
        //return str;
    }

    private static Boolean isTier(Object str) {
        if (str != null) {
            if ("R0".equals(str.toString()) || "R1".equals(str.toString()) || "R2".equals(str.toString()) || "R3".equals(str.toString())) {
                return true;
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
    }
}
