# ReplayOptions


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**model** | **str** |  | [optional] 
**temperature** | **float** |  | [optional] 
**bypass_cache** | **bool** |  | [optional] 

## Example

```python
from neurogate.neurogate.models.replay_options import ReplayOptions

# TODO update the JSON string below
json = "{}"
# create an instance of ReplayOptions from a JSON string
replay_options_instance = ReplayOptions.from_json(json)
# print the JSON string representation of the object
print(ReplayOptions.to_json())

# convert the object into a dict
replay_options_dict = replay_options_instance.to_dict()
# create an instance of ReplayOptions from a dict
replay_options_form_dict = replay_options.from_dict(replay_options_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


