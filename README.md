# kkh-roster
kkh doctors rostering app


1. Install java jdk.
    https://www.oracle.com/technetwork/java/javase/downloads/index.html
2. Create credentials.json file and put under /src/main/resources/ folder.
3. Authorize the app to access your google sheets.
4. Create a copy of the source sheet, this will be called the target sheet.
5. Run the program.

Program parameter
C:\GitHub\kkh-roster\build\libs>java -jar /GitHub/kkh-roster/build/libs/kkh_roster-1.0-SNAPSHOT-uber.jar <fromDate yyyy-MM-dd>to<toDateyyyy-MM-dd> <sourceSpreadSheetId> <targetSpreadSheetId> <optional and defaulted to test!A3:AG range>
Sample below:
C:\GitHub\kkh-roster\build\libs>java -jar /GitHub/kkh-roster/build/libs/kkh_roster-1.0-SNAPSHOT-uber.jar 2019-01-01to2019-01-31 1wIjFxOuctD-f0s9gG3f0QDf2QmkS1tXwT3o5hBU__-0 1psqPSDk16rpZBrQ-CP1_NS_-eDf4BLIG95ehdk_auCQ test!A3:AG



