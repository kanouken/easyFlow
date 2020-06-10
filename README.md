# easyFlow
an simple json based java flow framework
一个简单的基于json文件配置的 工作流框架

# feature
    

# how to use


@Autowired
	EasyFlowEngine engine;

	@Autowired
	UserService userService;

	@Test
	public void test() throws FileNotFoundException {

		ObjectMapper om = new ObjectMapper();

		om.configure(Feature.WRITE_NUMBERS_AS_STRINGS, true);
		om.configure(Feature.QUOTE_NON_NUMERIC_NUMBERS, true);
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		om.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
		om.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

		EasyFlowContext context = new EasyFlowContext();
		context.put("userService", userService);
		JsonFlowReader reader = new JsonFlowReader(om);
		JsonFlowFactory factory = new JsonFlowFactory(reader);
		List<JsonFlowNode> flow = factory.createFlow(new FileReader("src/main/resources/myFlow.json"));
		EasyFlowInstance start = engine.start(flow, context);
	}

```
{
	"name": "请假申请",
	"description": "请假流程",
	"key": "myFirstFlow",
	"nodes": 
	[
		{
			"name": "start",
			"type": "start",
			"nextNode": "node1"
		},

		{
			"name": "node1",
			"description": "提交申请",
			"type": "task",
			"nextNode": "node2"
		},

		{
			"name": "node2",
			"description": "领导审批",
			"type": "task",
			"assignments": "inputUser",
			"nextNode": "node3"
		},

		{
			"name": "node3",
			"type": "end"
		}
	]
}
```
