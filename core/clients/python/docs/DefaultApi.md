# neurogate.DefaultApi

All URIs are relative to *http://localhost:8080/api*

Method | HTTP request | Description
------------- | ------------- | -------------
[**chat_completions**](DefaultApi.md#chat_completions) | **POST** /chat/completions | Send a chat completion request
[**replay_session**](DefaultApi.md#replay_session) | **POST** /debug/sessions/{sessionId}/replay | Replay a debug session


# **chat_completions**
> ChatResponse chat_completions(chat_request)

Send a chat completion request

### Example


```python
import neurogate
from neurogate.models.chat_request import ChatRequest
from neurogate.models.chat_response import ChatResponse
from neurogate.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost:8080/api
# See configuration.py for a list of all supported configuration parameters.
configuration = neurogate.Configuration(
    host = "http://localhost:8080/api"
)


# Enter a context with an instance of the API client
with neurogate.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = neurogate.DefaultApi(api_client)
    chat_request = neurogate.ChatRequest() # ChatRequest | 

    try:
        # Send a chat completion request
        api_response = api_instance.chat_completions(chat_request)
        print("The response of DefaultApi->chat_completions:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling DefaultApi->chat_completions: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **chat_request** | [**ChatRequest**](ChatRequest.md)|  | 

### Return type

[**ChatResponse**](ChatResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Successful response |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **replay_session**
> ChatResponse replay_session(session_id, replay_options=replay_options)

Replay a debug session

### Example


```python
import neurogate
from neurogate.models.chat_response import ChatResponse
from neurogate.models.replay_options import ReplayOptions
from neurogate.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost:8080/api
# See configuration.py for a list of all supported configuration parameters.
configuration = neurogate.Configuration(
    host = "http://localhost:8080/api"
)


# Enter a context with an instance of the API client
with neurogate.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = neurogate.DefaultApi(api_client)
    session_id = 'session_id_example' # str | 
    replay_options = neurogate.ReplayOptions() # ReplayOptions |  (optional)

    try:
        # Replay a debug session
        api_response = api_instance.replay_session(session_id, replay_options=replay_options)
        print("The response of DefaultApi->replay_session:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling DefaultApi->replay_session: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **session_id** | **str**|  | 
 **replay_options** | [**ReplayOptions**](ReplayOptions.md)|  | [optional] 

### Return type

[**ChatResponse**](ChatResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Replay result |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

