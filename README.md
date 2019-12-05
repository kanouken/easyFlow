# easyFlow
an simple java flow framework


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
