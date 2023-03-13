package com.function;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class JsonRead {

    @FunctionName("JsonRead")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse the JSON request body
        String requestBody = request.getBody().orElse(null);

        if (requestBody == null || requestBody.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("pass a valid JSON ")
                    .build();
        } else {
            
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body("requestBody : coucou")
                    .build();
        }
    }
}
