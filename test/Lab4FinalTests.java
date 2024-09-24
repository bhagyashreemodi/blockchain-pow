package test;

import java.util.HashMap;
import java.util.Map;

import static common.FormattedSystemOut.setupFormattedSysOut;

public class Lab4FinalTests {

    public static void main(String[] args) {
        setupFormattedSysOut();
        Map<Test, Integer> tests = new HashMap<>();
        Map<String, String> testResults = new HashMap<>();
        tests.put(new TestInitialSetup(), 15);
        tests.put(new TestInvalidTransaction(), 15);
        tests.put(new TestFirstBlockMining(), 30);
        tests.put(new TestBlockConsensus(), 40);
        tests.put(new TestNodeFailureResilience(), 50);
        tests.put(new TestOutdatedInformationRejection(), 50);
        int totalPoints = 0;
        for(Map.Entry<Test, Integer> entry : tests.entrySet()) {
            Test test = entry.getKey();
            int score = entry.getValue();
            try {
                test.perform();
                System.out.println("Test passed: " + test.getClass().getName() + " (" + score + " points)");
                testResults.put(test.getClass().getName(), "PASSED");
                totalPoints += score;
            } catch (Exception e) {
                testResults.put(test.getClass().getName(), "FAILED");
                System.out.println("Test failed: " + test.getClass().getName());
            }
        }
        System.out.println("Test results:");
        for(Map.Entry<String, String> entry : testResults.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        System.out.println("Total points: " + totalPoints);
        if(totalPoints == 200) {
            System.out.println("All tests passed!");
            System.exit(0);
        } else {
            System.out.println("Some tests failed. Please check the output above for more details.");
            System.exit(2);
        }
    }
}
