# kkh-roster
kkh doctors rostering app


1. Install java jdk.
    https://www.oracle.com/technetwork/java/javase/downloads/index.html
2. Install intellij community edition.
    https://www.jetbrains.com/idea/download
3. Create credentials.json file and put under /src/main/resources/ folder.
4. Authorize the app to access your google sheets.
5. Create a copy of the source sheet, this will be called the target sheet.
6. Google sheet is assigned an id. Edit Main.java put the 3 program parameters.

    String date = "**2019-01-01**";
    
    final String sourceSpreadsheetId = "**1wIjFxOuctD-f0s9gG3f0QDf2QmkS1tXwT3o5hBU__-0**";

    final String targetSpreadsheetId = "**1psqPSDk16rpZBrQ-CP1_NS_-eDf4BLIG95ehdk_auCQ**";
7. Run the program.

C:\GitHub\kkh-roster\build\libs>java -jar /GitHub/kkh-roster/build/libs/kkh_roster-1.0-SNAPSHOT-uber.jar <yyyy-MM-dd> <sourceSpreadSheetId> <targetSpreadSheetId>

C:\GitHub\kkh-roster\build\libs>java -jar /GitHub/kkh-roster/build/libs/kkh_roster-1.0-SNAPSHOT-uber.jar 2019-01-01 1wIjFxOuctD-f0s9gG3f0QDf2QmkS1tXwT3o5hBU__-0 1psqPSDk16rpZBrQ-CP1_NS_-eDf4BLIG95ehdk_auCQ



