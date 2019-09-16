# kkh-roster
kkh doctors rostering app


1. Install java jdk.
    https://www.oracle.com/technetwork/java/javase/downloads/index.html
2. Create credentials.json file and put under /src/main/resources/ folder.
3. Authorize the app to access your google sheets.
4. Create a copy of the source sheet, this will be called the target sheet.
5. Run the program.

Program parameter

C:\GitHub\kkh-roster\build\libs>java -jar /GitHub/kkh-roster/build/libs/kkh_roster-1.0-SNAPSHOT-uber.jar <fromDate yyyy-MM-dd>to<toDateyyyy-MM-dd> <sourceSpreadSheetId> <targetSpreadSheetId> <sourceSpreadSheetRange> <target2SpreadSheetId> <target2SpreadSheetRange>

Sample below:

C:\GitHub\kkh-roster\build\libs>java -jar /GitHub/kkh-roster/build/libs/kkh_roster-1.0-SNAPSHOT-uber.jar **2019-07-02to2019-08-04** **1P3u-LNPo05yMlhYveFEcb4M43wAAMeraOXtN4aUN2kE** **1nXjLp8ZFsNjHqyn8nzPUuu5MVklVNcdnPzcpjyuWPHU** **July!A4:AJ** **1jvCYffTi4Whr6saDuPDZfr8xViB1yxW-HgOxjpt2gRQ** **July 2019!A4:AI**



