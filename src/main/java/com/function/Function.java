package com.function;

import com.microsoft.azure.functions.annotation.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Optional;

import com.microsoft.azure.functions.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {

    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Read the file path from the query parameter
        String filePath = "C:\\Users\\t764778\\Documents\\Mydev\\test.json";

       /*  if (filePath == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Please provide a file path in the request query parameters.")
                    .build();
        }*/

        // Read the file content
        StringBuilder contentBuilder = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append("\n");
            }
            reader.close();
        } catch (Exception e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Failed to read the file. Error message: " + e.getMessage())
                    .build();
        }

        // Return the file content as a JSON response
        String responseContent = contentBuilder.toString();
        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(responseContent)
                .build();
    }
}
