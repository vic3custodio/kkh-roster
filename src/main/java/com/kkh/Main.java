package com.kkh;

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
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.List;

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

    private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    //private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
    private static final List<String> SCOPES = new ArrayList<>();//Collections.singletonList(SheetsScopes.DRIVE,SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    static {
        SCOPES.add(SheetsScopes.DRIVE);
        SCOPES.add(SheetsScopes.DRIVE_FILE);
        SCOPES.add(SheetsScopes.SPREADSHEETS);
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
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
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

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        String date = "2019-01-01";
        final String sourceSpreadsheetId = "1wIjFxOuctD-f0s9gG3f0QDf2QmkS1tXwT3o5hBU__-0";
        final String targetSpreadsheetId = "1psqPSDk16rpZBrQ-CP1_NS_-eDf4BLIG95ehdk_auCQ";
        final String range = "test!A3:AF";

        if (args == null || args.length < 1) {
            args = new String[4];
            args[0] = date;
            args[1] = sourceSpreadsheetId;
            args[2] = targetSpreadsheetId;
            args[3] = range;
        }

        generateC(args);
    }

    public static void generateC(String[] args) throws IOException, GeneralSecurityException {

        // fill the sheet with known information (doctor's name, type, color, days leave, C, filled)

        Set<String> skip = new HashSet<>();
        skip.add("Events");
        skip.add("Teaching");
        skip.add("Department Juniors");
        skip.add("Residents");
        skip.add("TOTAL NUMBER ON LEAVE");
        //skip.add("No of calls");

        String date = args[0];
        //default, ISO_LOCAL_DATE
        LocalDate localDate = LocalDate.parse(date);
        Month month = localDate.getMonth();
        Integer daysOfTheMonth = month.length(localDate.isLeapYear());

        // scan the sheet once to take the inputted information.
        List<Doctor> doctors = new ArrayList<>();

        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        //final String spreadsheetId = "1T2zC68QRSD3WXrmJczcu3-ewJ6bXabBmskLepM1Ub7A";
        final String sourceSpreadsheetId = args[1];

        final String range = args[3];
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        ValueRange response = service.spreadsheets().values()
                .get(sourceSpreadsheetId, range)
                .execute();

        Map<Integer, Day> dayMap = new HashMap<>();

        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
        } else {
            for (List row : values) {
                Doctor doctor = null;
                Day day = null;
                for (int i=0; i<=daysOfTheMonth; i++) {
                    if (dayMap.containsKey(Integer.valueOf(i))) {
                        day = dayMap.get(Integer.valueOf(i));
                    }
                    else {
                        dayMap.put(Integer.valueOf(i), new Day(Integer.valueOf(i)));
                    }
                    if (row.size() > i) {
                        if (i == 0) { // first column is the name
                            String name = row.get(i).toString();
                            if (skip.contains(name)) {
                                break;
                            }
                            doctor = new Doctor(name);
                            if (name.indexOf("B-") == 0) {
                                doctor.setColor("BLUE");
                            }
                            doctors.add(doctor);
                        }
                        else { // succeeding columns is the cell
                            Cell cell = new Cell();
                            setCell(doctor, day, cell, row.get(i).toString());
                            doctor.getCells().add(cell);
                        }
                    }
                    else {
                        if (doctor != null) {
                            Cell cell = new Cell();
                            doctor.getCells().add(cell);
                        }
                    }
                }
                if (doctor != null) {
                    System.out.println("");
                    System.out.printf("Doctor Name: %s, Cell size: %s\n", doctor.getName(), doctor.getCells().size());
                }
            }
        }

        // scan the sheet second time to generate the C

        List<Doctor> sortedDoctors = new ArrayList<>(doctors);
        //List<Doctor> sortedDoctors = new ArrayList<>();//(doctors);
        //sortedDoctors.addAll(doctors);
        Collections.sort(sortedDoctors, new Comparator<Doctor>() {
            public int compare(Doctor d1, Doctor d2) {
                return d2.getCurrentTotalNumberOfLs().compareTo(d1.getCurrentTotalNumberOfLs());
            }
        });

        System.out.println("");
        System.out.printf("First doctor on the list: %s", sortedDoctors.get(0).getName());

        for (int i=0; i<daysOfTheMonth; i++) {
            //Day day = new Day(i+1);
            Day day = dayMap.get(Integer.valueOf(i+1));
            for (Doctor doctor: sortedDoctors) {
                Cell cell = doctor.getCells().get(i);
                if (cell.getValue() == null) {
                    //if (!cell.isFilled()) {
                        if (isSpaceBetweenTwoCMoreThanOne(doctor, i, daysOfTheMonth)) {
                            if (doctor.getCurrentTotalNumberOfCs() < 6) {
                                if (day.getTotalNumberOfDoctorOnC() < 3) {
                                    if (i < (daysOfTheMonth-1) && !doctor.getCells().get(i + 1).isL()) {
                                        if (DayOfWeek.SATURDAY.compareTo(localDate.getDayOfWeek()) != 0) {
                                            tryToAssignC(doctor, cell, day);
                                        } else {
                                            if (isNotCOnConsecutiveSat(doctor, i, daysOfTheMonth)) {
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

        // output the content to see
        System.out.println("|Doctor|001|002|003|004|005|006|007|008|009|010|011|012|013|014|015|016|017|018|019|020|021|022|023|024|025|026|027|028|029|030|031|TOT|");
        for (Doctor doctor: doctors) {
            String name = doctor.getName();
            if (name.length() >= 6) {
                name = name.substring(0, 6);
            }
            else {
                name = padWithSpace(name, 7);
            }
            String row = "|" + name + "|";
            for (int i=0; i<daysOfTheMonth+1; i++) {
                if (i == daysOfTheMonth) {
                    row = row + " " + doctor.getCurrentTotalNumberOfCs() + " |";
                }
                else {
                    row = row + " " + doctor.getCells().get(i).getValue() + " |";
                }
            }
            System.out.println(row);
        }
        List<Object> cTotals = new ArrayList<>();
        cTotals.add("");
        String total = "|TOTAL |";
        for (int i=0; i<daysOfTheMonth; i++) {
            Day day = dayMap.get(Integer.valueOf(i+1));
            total = total + " " + day.getTotalNumberOfDoctorOnC() + " |";
            cTotals.add("" + day.getTotalNumberOfDoctorOnC());
        }
        System.out.println(total);

        // copy the generated data to another sheet
        int j = 0;
        List<List<Object>> updatedValues = new ArrayList<>();
        for (List row : values) {
            boolean isSkip = false;
            for (int i = 0; i <= daysOfTheMonth; i++) {
                if (row.size() > i) {
                    if (i == 0) { // first column is the name
                        String name = row.get(i).toString();
                        if (skip.contains(name)) {
                            isSkip = true;
                            break;
                        }
                    }
                    else {
                        if (doctors.get(j).getCells().size() >= i-1) {
                            String value = doctors.get(j).getCells().get(i - 1).getValue();
                            if (value != null) {
                                row.set(i, doctors.get(j).getCells().get(i - 1).getValue());
                            }
                            else {
                                row.set(i, "");
                            }

                        }
                    }
                }
                else {
                    if (doctors.size() > j) {
                        String value = doctors.get(j).getCells().get(i - 1).getValue();
                        if (value != null) {
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
        updatedValues.add(cTotals);

        ValueRange body = new ValueRange().setValues(updatedValues);

        final String targetSpreadsheetId = args[2];

        service.spreadsheets().values().update(targetSpreadsheetId, range, body)
                //.setValueInputOption("USER_ENTERED")
                .setValueInputOption("RAW")
                .execute();

    }

    private static void setCell(Doctor doctor, Day day, Cell cell, String cellValue) {
        if (cellValue.indexOf("C") > -1) {
            System.out.printf("Pre-Setting doctor to %s Doctor Name: %s, Day: %s\n", cellValue, doctor.getName(), day.getDay());
            cell.setValue(cellValue);
            doctor.addCurrentTotalNumberOfCs();
            day.addTotalNumberOfDoctorOnC();
            if (doctor.getName().indexOf("B-") == 0) {
                day.addNumberOfBlueDoctorOnC();
            }
        }
        else if (cellValue.indexOf("L") > -1) {
            System.out.printf("Pre-Setting doctor to %s Doctor Name: %s, Day: %s\n", cellValue, doctor.getName(), day.getDay());
            cell.setValue(cellValue);
            doctor.addCurrentTotalNumberOfLs();
        }
        else if ("F".equalsIgnoreCase(cellValue)) {
            cell.setValue("F");
        }
    }

    private static void tryToAssignC(Doctor doctor, Cell cell, Day day) {
        if (!"BLUE".equalsIgnoreCase(doctor.getColor())) {
            System.out.printf("Setting non-blue doctor to C Doctor Name: %s, Day: %s\n", doctor.getName(), day.getDay());
            cell.setValue("C");
            doctor.addCurrentTotalNumberOfCs();
            day.addTotalNumberOfDoctorOnC();
        } else {
            if (day.getNumberOfBlueDoctorOnC() < 1) {
                System.out.printf("Setting blue doctor to C Doctor Name: %s, Day: %s\n", doctor.getName(), day.getDay());
                cell.setValue("C");
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
        for (int i=0; i<=(fixedLength-str.length()); i++) {
            str = str + " ";
        }
        return str;
    }
}
