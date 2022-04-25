package main;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskRequest;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskResult;
import com.amazonaws.services.stepfunctions.model.SendTaskFailureRequest;
import com.amazonaws.services.stepfunctions.model.SendTaskSuccessRequest;
import com.amazonaws.util.json.Jackson;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.concurrent.TimeUnit;

public class LocalCode {

    public static final String ACTIVITY_ARN = "TO_BE_ADDED";

    public String getLocalCode(Integer value) {
        System.out.println("Received payload with number: " + value);
        return "{\"Code\": \"" + value + "\"}";
    }

    public static void main(final String[] args) {
        LocalCode code = new LocalCode();
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSocketTimeout((int) TimeUnit.SECONDS.toMillis(70));

        AWSStepFunctions client = AWSStepFunctionsClientBuilder.standard()
                .withRegion(Regions.EU_CENTRAL_1)
                .withCredentials(new ProfileCredentialsProvider())
                .withClientConfiguration(clientConfiguration)
                .build();

        System.out.println("STARTED listening for the Activity payload!");
        try {
            while (true) {
                GetActivityTaskResult getActivityTaskResult =
                        client.getActivityTask(new GetActivityTaskRequest().withActivityArn(ACTIVITY_ARN));

                if (getActivityTaskResult.getTaskToken() != null) {
                    System.out.println("Found result from previous step.");
                    try {
                        JsonNode json = Jackson.jsonNodeOf(getActivityTaskResult.getInput());
                        String result = code.getLocalCode(json.get("value").intValue());
                        client.sendTaskSuccess(
                                new SendTaskSuccessRequest().withOutput(
                                        result).withTaskToken(getActivityTaskResult.getTaskToken()));
                    } catch (Exception e) {
                        client.sendTaskFailure(new SendTaskFailureRequest().withTaskToken(getActivityTaskResult.getTaskToken()));
                    }
                } else {
                    Thread.sleep(1000);
                }
            }
        } catch (Exception e) {
            System.out.println("EXCEPTION while listening for the Activity payload!");
            e.printStackTrace();
        } finally {
            System.out.println("STOPPED listening for the Activity payload!");
        }
    }
}
