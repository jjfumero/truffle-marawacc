
package cz.cuni.mff.d3s.trupple.language.parser;

import cz.cuni.mff.d3s.trupple.language.nodes.*;
import cz.cuni.mff.d3s.trupple.language.customtypes.IOrdinalType;

import com.oracle.truffle.api.source.Source;

import java.util.ArrayList;
import java.util.List;

public class Parser{
	public static final int _EOF = 0;
	public static final int _identifier = 1;
	public static final int _stringLiteral = 2;
	public static final int _numericLiteral = 3;
	public static final int _doubleLiteral = 4;
	public static final int maxT = 62;

	static final boolean _T = true;
	static final boolean _x = false;
	static final int minErrDist = 2;

	public Token t;    // last recognized token
	public Token la;   // lookahead token
	int errDist = minErrDist;
	
	public Scanner scanner;
	public Errors errors;
	private final NodeFactory factory;
    public PascalRootNode mainNode;
	private int loopDepth = 0;

	

	public Parser() {
		this.factory = new NodeFactory(this);
		errors = new Errors();
	}

	void SynErr (int n) {
		if (errDist >= minErrDist) errors.SynErr(la.line, la.col, n);
		errDist = 0;
	}

	public void SemErr (String msg) {
		if (errDist >= minErrDist) errors.SemErr(t.line, t.col, msg);
		errDist = 0;
	}
	
	void Get () {
		for (;;) {
			t = la;
			la = scanner.Scan();
			if (la.kind <= maxT) {
				++errDist;
				break;
			}

			la = t;
		}
	}
	
	void Expect (int n) {
		if (la.kind==n) Get(); else { SynErr(n); }
	}
	
	boolean StartOf (int s) {
		return set[s][la.kind];
	}
	
	void ExpectWeak (int n, int follow) {
		if (la.kind == n) Get();
		else {
			SynErr(n);
			while (!StartOf(follow)) Get();
		}
	}
	
	boolean WeakSeparator (int n, int syFol, int repFol) {
		int kind = la.kind;
		if (kind == n) { Get(); return true; }
		else if (StartOf(repFol)) return false;
		else {
			SynErr(n);
			while (!(set[syFol][kind] || set[repFol][kind] || set[0][kind])) {
				Get();
				kind = la.kind;
			}
			return StartOf(syFol);
		}
	}
	
	void Pascal() {
		if (StartOf(1)) {
			if (la.kind == 5) {
				ImportsSection();
			}
			while (StartOf(2)) {
				Declaration();
			}
			MainFunction();
		} else if (la.kind == 59) {
			Unit();
		} else SynErr(63);
	}

	void ImportsSection() {
		Expect(5);
		Expect(1);
		factory.importUnit(t); 
		while (la.kind == 6) {
			Get();
			Expect(1);
			factory.importUnit(t); 
		}
		Expect(7);
	}

	void Declaration() {
		if (la.kind == 24) {
			VariableDefinitions();
		} else if (la.kind == 12) {
			ConstantDefinition();
		} else if (la.kind == 8) {
			TypeDefinition();
		} else if (la.kind == 32 || la.kind == 33) {
			Subroutine();
		} else SynErr(64);
	}

	void MainFunction() {
		factory.startMainFunction(); 
		StatementNode blockNode = Block();
		mainNode = factory.finishMainFunction(blockNode); 
		Expect(35);
	}

	void Unit() {
		Expect(59);
		Expect(1);
		factory.startUnit(t); 
		Expect(7);
		Expect(60);
		InterfaceSection();
		Expect(61);
		factory.leaveUnitInterfaceSection(); 
		ImplementationSection();
		Expect(37);
		factory.endUnit(); 
		Expect(35);
	}

	void VariableDefinitions() {
		Expect(24);
		VariableLineDefinition();
		Expect(7);
		while (la.kind == 1) {
			VariableLineDefinition();
			Expect(7);
		}
	}

	void ConstantDefinition() {
		Expect(12);
		Expect(1);
		Token identifier = t; 
		Expect(9);
		if (StartOf(3)) {
			NumericConstant value = NumericConstant();
			factory.createNumericConstant(identifier, value); 
		} else if (la.kind == 1 || la.kind == 2) {
			String value = StringConstant();
			factory.createStringOrCharConstant(identifier, value); 
		} else if (StartOf(4)) {
			boolean value = LogicConstant();
			factory.createBooleanConstant(identifier, value); 
		} else if (la.kind == 1) {
			Get();
			factory.createObjectConstant(identifier, t); 
		} else SynErr(65);
		Expect(7);
	}

	void TypeDefinition() {
		Expect(8);
		Expect(1);
		Token identifier = t; 
		Expect(9);
		Enum(identifier);
		Expect(7);
	}

	void Subroutine() {
		boolean isFunction = false; 
		if (la.kind == 32) {
			Get();
			isFunction = true; 
		} else if (la.kind == 33) {
			Get();
		} else SynErr(66);
		Expect(1);
		if(isFunction) factory.startFunction(t); else factory.startProcedure(t); 
		Token name = t; 
		Token returnType = null; 
		List<FormalParameter> formalParameters = new ArrayList<>(); 
		if (la.kind == 10) {
			formalParameters = FormalParameterList();
		}
		factory.addFormalParameters(formalParameters); 
		if (isFunction) {
			Expect(25);
			Expect(1);
			factory.setFunctionReturnValue(t); 
			returnType = t; 
		}
		if(isFunction)  factory.finishFormalParameterListFunction(name, formalParameters,t.val.toLowerCase()); 
		else factory.finishFormalParameterListProcedure(name, formalParameters); 
		Expect(7);
		if (la.kind == 34) {
			Get();
			if(isFunction) factory.addFunctionInterface(name, formalParameters, returnType.val.toLowerCase()); 
			else factory.addProcedureInterface(name, formalParameters); 
		} else if (StartOf(5)) {
			while (StartOf(2)) {
				Declaration();
			}
			StatementNode bodyNode = Block();
			if(isFunction) factory.finishFunction(bodyNode); 
			else factory.finishProcedure(bodyNode); 
			Expect(7);
		} else SynErr(67);
	}

	void Enum(Token identifier) {
		Expect(10);
		List<String> identifiers = new ArrayList<String>(); 
		Expect(1);
		identifiers.add(t.val.toLowerCase()); 
		while (la.kind == 6) {
			Get();
			Expect(1);
			identifiers.add(t.val.toLowerCase()); 
		}
		Expect(11);
		factory.registerEnumType(identifier.val.toLowerCase(), identifiers); 
	}

	NumericConstant  NumericConstant() {
		NumericConstant  value;
		value = null; 
		if (la.kind == 16 || la.kind == 17) {
			if (la.kind == 16) {
				Get();
			} else {
				Get();
			}
			Token signToken = t; 
			NumericConstant signedValue = NumericConstant();
			value = factory.createUnsignedConstant(signedValue, signToken); 
		} else if (la.kind == 1 || la.kind == 3 || la.kind == 4) {
			value = UnsignedNumericArithmeticConstant();
		} else SynErr(68);
		return value;
	}

	String  StringConstant() {
		String  value;
		value = StringValue();
		while (la.kind == 16) {
			Get();
			String rvalue = StringValue();
			value = value.concat(rvalue); 
		}
		return value;
	}

	boolean  LogicConstant() {
		boolean  value;
		value = LogicTermConstant();
		while (la.kind == 13) {
			Get();
			boolean rvalue = LogicTermConstant();
			value = value || rvalue; 
		}
		return value;
	}

	boolean  LogicTermConstant() {
		boolean  value;
		value = LogicFactorConstant();
		while (la.kind == 14) {
			Get();
			boolean rvalue = LogicFactorConstant();
			value = value && rvalue; 
		}
		return value;
	}

	boolean  LogicFactorConstant() {
		boolean  value;
		value = false; 
		if (la.kind == 15) {
			Get();
			boolean negatedValue = LogicFactorConstant();
			value = !negatedValue; 
		} else if (la.kind == 1 || la.kind == 22 || la.kind == 23) {
			value = LogicSingleConstant();
		} else SynErr(69);
		return value;
	}

	boolean  LogicSingleConstant() {
		boolean  value;
		value = false; 
		if (la.kind == 22 || la.kind == 23) {
			value = LogicLiteral();
		} else if (la.kind == 1) {
			Get();
			value = factory.getBooleanConstant(t); 
		} else SynErr(70);
		return value;
	}

	boolean  LogicLiteral() {
		boolean  result;
		result = false; 
		if (la.kind == 22) {
			Get();
			result = true; 
		} else if (la.kind == 23) {
			Get();
			result = false; 
		} else SynErr(71);
		return result;
	}

	String  StringValue() {
		String  value;
		value = ""; 
		if (la.kind == 2) {
			Get();
			value = factory.createStringFromLiteral(t); 
		} else if (la.kind == 1) {
			Get();
			value = factory.getStringConstant(t); 
		} else SynErr(72);
		return value;
	}

	NumericConstant  UnsignedNumericArithmeticConstant() {
		NumericConstant  value;
		value = NumericTermConstant();
		while (la.kind == 16 || la.kind == 17) {
			if (la.kind == 16) {
				Get();
			} else {
				Get();
			}
			Token opToken = t; 
			NumericConstant rvalue = NumericTermConstant();
			value = factory.createNumericConstantFromBinary(value, rvalue, opToken); 
		}
		return value;
	}

	NumericConstant  NumericTermConstant() {
		NumericConstant  value;
		value = NumericFactorConstant();
		while (StartOf(6)) {
			if (la.kind == 18) {
				Get();
			} else if (la.kind == 19) {
				Get();
			} else if (la.kind == 20) {
				Get();
			} else {
				Get();
			}
			Token opToken = t; 
			NumericConstant rvalue = NumericFactorConstant();
			value = factory.createNumericConstantFromBinary(value, rvalue, opToken); 
		}
		return value;
	}

	NumericConstant  NumericFactorConstant() {
		NumericConstant  value;
		value = null; 
		if (la.kind == 3) {
			Get();
			value = new NumericConstant(Long.parseLong(t.val), false); 
		} else if (la.kind == 4) {
			Get();
			value = new NumericConstant(Double.parseDouble(t.val), true); 
		} else if (la.kind == 1) {
			Get();
			value = factory.getNumericConstant(t); 
		} else SynErr(73);
		return value;
	}

	void VariableLineDefinition() {
		List<String> identifiers = new ArrayList<>(); 
		Expect(1);
		identifiers.add(t.val.toLowerCase()); 
		while (la.kind == 6) {
			Get();
			Expect(1);
			identifiers.add(t.val.toLowerCase()); 
		}
		Expect(25);
		if (la.kind == 1) {
			Get();
			factory.finishVariableLineDefinition(identifiers, t); 
		} else if (la.kind == 27 || la.kind == 28) {
			List<IOrdinalType> ordinalDimensions  = ArrayDefinition();
			while (continuesArray()) {
				Expect(26);
				List<IOrdinalType> additionalDimensions  = ArrayDefinition();
				ordinalDimensions.addAll(additionalDimensions); 
			}
			Expect(26);
			Expect(1);
			factory.finishArrayDefinition(identifiers, ordinalDimensions, t); 
		} else SynErr(74);
	}

	List<IOrdinalType>  ArrayDefinition() {
		List<IOrdinalType>  ordinalDimensions;
		if (la.kind == 27) {
			Get();
		}
		Expect(28);
		ordinalDimensions = new ArrayList<>(); 
		Expect(29);
		IOrdinalType ordinal = null; 
		ordinal = Ordinal();
		ordinalDimensions.add(ordinal); 
		while (la.kind == 6) {
			Get();
			ordinal = Ordinal();
			ordinalDimensions.add(ordinal); 
		}
		Expect(30);
		return ordinalDimensions;
	}

	IOrdinalType  Ordinal() {
		IOrdinalType  ordinal;
		ordinal = null; 
		if (la.kind == 3) {
			Get();
			Token lowerBound = t; 
			Expect(31);
			Expect(3);
			Token upperBound = t; 
			ordinal = factory.createSimpleOrdinal(lowerBound, upperBound); 
		} else if (la.kind == 1) {
			Get();
			Token identifier = t; 
			ordinal = factory.createSimpleOrdinalFromTypename(identifier); 
		} else SynErr(75);
		return ordinal;
	}

	List<FormalParameter>  FormalParameterList() {
		List<FormalParameter>  formalParameters;
		Expect(10);
		formalParameters = new ArrayList<>(); 
		List<FormalParameter> parameter = new ArrayList<>(); 
		parameter = FormalParameter();
		factory.appendFormalParameter(parameter, formalParameters); 
		while (la.kind == 7) {
			Get();
			parameter = FormalParameter();
			factory.appendFormalParameter(parameter, formalParameters); 
		}
		Expect(11);
		return formalParameters;
	}

	StatementNode  Block() {
		StatementNode  blockNode;
		Expect(36);
		List<StatementNode> body = new ArrayList<>(); 
		if (StartOf(7)) {
			StatementSequence(body);
		}
		Expect(37);
		blockNode = factory.finishBlock(body); 
		return blockNode;
	}

	List<FormalParameter>  FormalParameter() {
		List<FormalParameter>  formalParameter;
		List<String> identifiers = new ArrayList<>(); 
		boolean isOutput = false; 
		if (la.kind == 24) {
			Get();
			isOutput = true; 
		}
		Expect(1);
		identifiers.add(t.val.toLowerCase()); 
		while (la.kind == 6) {
			Get();
			Expect(1);
			identifiers.add(t.val.toLowerCase()); 
		}
		Expect(25);
		Expect(1);
		formalParameter = factory.createFormalParametersList(identifiers, t.val.toLowerCase(), isOutput); 
		return formalParameter;
	}

	void StatementSequence(List<StatementNode> body ) {
		StatementNode statement = Statement();
		body.add(statement); 
		while (la.kind == 7) {
			Get();
			statement = Statement();
			body.add(statement); 
		}
	}

	StatementNode  Statement() {
		StatementNode  statement;
		statement = null; 
		switch (la.kind) {
		case 7: case 37: case 42: case 49: {
			statement = factory.createEmptyStatement(); 
			break;
		}
		case 1: case 2: case 3: case 4: case 10: case 15: case 16: case 17: case 22: case 23: case 58: {
			statement = Expression();
			break;
		}
		case 51: {
			statement = IfStatement();
			break;
		}
		case 43: {
			statement = ForLoop();
			break;
		}
		case 50: {
			statement = WhileLoop();
			break;
		}
		case 48: {
			statement = RepeatLoop();
			break;
		}
		case 41: {
			statement = CaseStatement();
			break;
		}
		case 38: {
			Get();
			if(loopDepth == 0) 
			SemErr("Break statement outside of a loop."); 
			statement = factory.createBreak(); 
			break;
		}
		case 36: {
			statement = Block();
			break;
		}
		case 40: {
			statement = ReadStatement();
			break;
		}
		case 39: {
			Get();
			statement = factory.createRandomizeNode(); 
			break;
		}
		default: SynErr(76); break;
		}
		return statement;
	}

	ExpressionNode  Expression() {
		ExpressionNode  expression;
		expression = LogicTerm();
		while (la.kind == 13) {
			Get();
			Token op = t; 
			ExpressionNode right = LogicTerm();
			expression = factory.createBinary(op, expression, right); 
		}
		return expression;
	}

	StatementNode  IfStatement() {
		StatementNode  statement;
		Expect(51);
		ExpressionNode condition = Expression();
		Expect(52);
		StatementNode thenStatement = Statement();
		StatementNode elseStatement = null; 
		if (la.kind == 42) {
			Get();
			elseStatement = Statement();
		}
		statement = factory.createIfStatement(condition, thenStatement, elseStatement); 
		return statement;
	}

	StatementNode  ForLoop() {
		StatementNode  statement;
		loopDepth++; 
		Expect(43);
		boolean ascending = true; 
		Expect(1);
		Token variableToken = t; 
		Expect(44);
		ExpressionNode startValue = Expression();
		if (la.kind == 45) {
			Get();
			ascending = true; 
		} else if (la.kind == 46) {
			Get();
			ascending = false; 
		} else SynErr(77);
		ExpressionNode finalValue = Expression();
		Expect(47);
		StatementNode loopBody = Statement();
		statement = factory.createForLoop(ascending, variableToken, startValue, finalValue, loopBody); 
		loopDepth--; 
		return statement;
	}

	StatementNode  WhileLoop() {
		StatementNode  statement;
		loopDepth++; 
		Expect(50);
		ExpressionNode condition = Expression();
		Expect(47);
		StatementNode loopBody = Statement();
		statement = factory.createWhileLoop(condition, loopBody); 
		loopDepth--; 
		return statement;
	}

	StatementNode  RepeatLoop() {
		StatementNode  statement;
		loopDepth++; 
		Expect(48);
		List<StatementNode> bodyNodes = new ArrayList<>(); 
		StatementSequence(bodyNodes);
		Expect(49);
		ExpressionNode condition = Expression();
		statement = factory.createRepeatLoop(condition, factory.finishBlock(bodyNodes)); 
		loopDepth--; 
		return statement;
	}

	StatementNode  CaseStatement() {
		StatementNode  statement;
		Expect(41);
		ExpressionNode caseIndex = Expression();
		Expect(26);
		factory.startCaseList();	
		CaseList();
		Expect(37);
		statement = factory.finishCaseStatement(caseIndex); 
		return statement;
	}

	StatementNode  ReadStatement() {
		StatementNode  statement;
		Expect(40);
		statement = null; 
		if (StartOf(8)) {
			statement = factory.createReadLine(); 
		} else if (la.kind == 10) {
			Get();
			List<String> identifiers = new ArrayList<>(); 
			if (la.kind == 11) {
				Get();
				statement = factory.createReadLine(); 
			} else if (la.kind == 1) {
				Get();
				identifiers.add(t.val.toLowerCase()); 
				while (la.kind == 6) {
					Get();
					Expect(1);
					identifiers.add(t.val.toLowerCase()); 
				}
				Expect(11);
				statement = factory.createReadLine(identifiers); 
			} else SynErr(78);
		} else SynErr(79);
		return statement;
	}

	void CaseList() {
		ExpressionNode caseConstant = Expression();
		Expect(25);
		StatementNode caseStatement = Statement();
		factory.addCaseOption(caseConstant, caseStatement); 
		while (!caseEnds()) {
			Expect(7);
			caseConstant = Expression();
			Expect(25);
			caseStatement = Statement();
			factory.addCaseOption(caseConstant, caseStatement); 
		}
		if (la.kind == 7) {
			Get();
		}
		StatementNode elseStatement = null; 
		if (la.kind == 42) {
			Get();
			elseStatement = Statement();
			factory.setCaseElse(elseStatement); 
		}
		if (la.kind == 7) {
			Get();
		}
	}

	ExpressionNode  LogicTerm() {
		ExpressionNode  expression;
		expression = SignedLogicFactor();
		while (la.kind == 14) {
			Get();
			Token op = t; 
			ExpressionNode right = SignedLogicFactor();
			expression = factory.createBinary(op, expression, right); 
		}
		return expression;
	}

	ExpressionNode  SignedLogicFactor() {
		ExpressionNode  expression;
		expression = null; 
		if (la.kind == 15) {
			Get();
			Token op = t; 
			ExpressionNode right = SignedLogicFactor();
			expression = factory.createUnary(op, right); 
		} else if (StartOf(9)) {
			expression = LogicFactor();
		} else SynErr(80);
		return expression;
	}

	ExpressionNode  LogicFactor() {
		ExpressionNode  expression;
		expression = Arithmetic();
		if (StartOf(10)) {
			switch (la.kind) {
			case 53: {
				Get();
				break;
			}
			case 54: {
				Get();
				break;
			}
			case 55: {
				Get();
				break;
			}
			case 56: {
				Get();
				break;
			}
			case 9: {
				Get();
				break;
			}
			case 57: {
				Get();
				break;
			}
			}
			Token op = t; 
			ExpressionNode right = Arithmetic();
			expression = factory.createBinary(op, expression, right); 
		}
		return expression;
	}

	ExpressionNode  Arithmetic() {
		ExpressionNode  expression;
		expression = Term();
		while (la.kind == 16 || la.kind == 17) {
			if (la.kind == 16) {
				Get();
			} else {
				Get();
			}
			Token op = t; 
			ExpressionNode right = Term();
			expression = factory.createBinary(op, expression, right); 
		}
		return expression;
	}

	ExpressionNode  Term() {
		ExpressionNode  expression;
		expression = SignedFactor();
		while (StartOf(6)) {
			if (la.kind == 18) {
				Get();
			} else if (la.kind == 19) {
				Get();
			} else if (la.kind == 20) {
				Get();
			} else {
				Get();
			}
			Token op = t; 
			ExpressionNode right = SignedFactor();
			expression = factory.createBinary(op, expression, right); 
		}
		return expression;
	}

	ExpressionNode  SignedFactor() {
		ExpressionNode  expression;
		expression = null; 
		if (la.kind == 16 || la.kind == 17) {
			if (la.kind == 16) {
				Get();
			} else {
				Get();
			}
			Token unOp = t; 
			expression = SignedFactor();
			expression = factory.createUnary(unOp, expression); 
		} else if (StartOf(11)) {
			expression = Factor();
		} else SynErr(81);
		return expression;
	}

	ExpressionNode  Factor() {
		ExpressionNode  expression;
		expression = null; 
		switch (la.kind) {
		case 58: {
			expression = Random();
			break;
		}
		case 1: {
			Get();
			if (la.kind == 10 || la.kind == 29 || la.kind == 44) {
				expression = MemberExpression(t);
			} else if (StartOf(12)) {
				expression = factory.readSingleIdentifier(t); 
				if(expression == null) 
				SemErr("Undefined identifier " + t.val + "."); 
			} else SynErr(82);
			break;
		}
		case 10: {
			Get();
			expression = Expression();
			Expect(11);
			break;
		}
		case 2: {
			Get();
			expression = factory.createCharOrStringLiteral(t); 
			break;
		}
		case 4: {
			Get();
			expression = factory.createFloatLiteral(t); 
			break;
		}
		case 3: {
			Get();
			expression = factory.createNumericLiteral(t); 
			if(expression == null) 
			SemErr("Constant out of range!"); 
			break;
		}
		case 22: case 23: {
			boolean val; 
			val = LogicLiteral();
			expression = factory.createLogicLiteral(val); 
			break;
		}
		default: SynErr(83); break;
		}
		return expression;
	}

	ExpressionNode  Random() {
		ExpressionNode  expression;
		Expect(58);
		expression = null; 
		if (StartOf(12)) {
			expression = factory.createRandomNode(); 
		} else if (la.kind == 10) {
			Get();
			if (la.kind == 11) {
				Get();
				expression = factory.createRandomNode(); 
			} else if (la.kind == 3) {
				Get();
				expression = factory.createRandomNode(t); 
				Expect(11);
			} else SynErr(84);
		} else SynErr(85);
		return expression;
	}

	ExpressionNode  MemberExpression(Token identifierName) {
		ExpressionNode  expression;
		expression = null; 
		if (la.kind == 10) {
			Get();
			ExpressionNode functionNode = factory.createFunctionNode(identifierName); 
			List<ExpressionNode> parameters = new ArrayList<>(); 
			ExpressionNode parameter; 
			if (StartOf(13)) {
				parameter = Expression();
				parameters.add(parameter); 
				while (la.kind == 6) {
					Get();
					parameter = Expression();
					parameters.add(parameter); 
				}
			}
			Expect(11);
			expression = factory.createCall(functionNode, parameters); 
		} else if (la.kind == 44) {
			Get();
			ExpressionNode value = Expression();
			if(identifierName == null) { 
			SemErr("Invalid assignment target!"); 
			} else { 
			expression = factory.createAssignment(identifierName, value); 
			if(expression == null) 
			SemErr("Undefined variable " + identifierName.val.toLowerCase() + "."); 
			} 
		} else if (la.kind == 29) {
			expression = ArrayAccessing(identifierName);
		} else SynErr(86);
		return expression;
	}

	ExpressionNode  ArrayAccessing(Token identifierName) {
		ExpressionNode  expression;
		expression = null; 
		List<ExpressionNode> indexingNodes = new ArrayList<>(); 
		ExpressionNode indexingNode = null; 
		Expect(29);
		indexingNode = ArrayIndex();
		indexingNodes.add(indexingNode); 
		while (la.kind == 6) {
			Get();
			indexingNode = ArrayIndex();
			indexingNodes.add(indexingNode); 
		}
		Expect(30);
		if (StartOf(12)) {
			expression = factory.createReadArrayValue(identifierName, indexingNodes); 
		} else if (la.kind == 44) {
			Get();
			ExpressionNode value = Expression();
			expression = factory.createArrayIndexAssignment( 
			identifierName, indexingNodes, value); 
		} else SynErr(87);
		return expression;
	}

	ExpressionNode  ArrayIndex() {
		ExpressionNode  indexingNode;
		indexingNode = null; 
		if (!isSingleIdentifier()) {
			ExpressionNode indexNode = Expression();
			indexingNode = indexNode; 
		} else if (la.kind == 1) {
			Get();
			indexingNode = factory.createIndexingNode(t); 
		} else SynErr(88);
		return indexingNode;
	}

	void InterfaceSection() {
		while (StartOf(14)) {
			if (la.kind == 33) {
				ProcedureHeading();
			} else if (la.kind == 32) {
				FunctionHeading();
			} else if (la.kind == 24) {
				VariableDefinitions();
			} else {
				TypeDefinition();
			}
		}
	}

	void ImplementationSection() {
		while (StartOf(14)) {
			if (la.kind == 32 || la.kind == 33) {
				Subroutine();
			} else if (la.kind == 24) {
				VariableDefinitions();
			} else {
				TypeDefinition();
			}
		}
	}

	void ProcedureHeading() {
		Expect(33);
		Expect(1);
		Token name = t; 
		List<FormalParameter> formalParameters = new ArrayList<>(); 
		if (la.kind == 10) {
			formalParameters = FormalParameterList();
		}
		factory.addProcedureInterface(name, formalParameters); 
		Expect(7);
	}

	void FunctionHeading() {
		Expect(32);
		Expect(1);
		Token name = t; 
		List<FormalParameter> formalParameters = new ArrayList<>(); 
		if (la.kind == 10) {
			formalParameters = FormalParameterList();
		}
		Expect(25);
		Expect(1);
		String returnValue = t.val; 
		factory.addFunctionInterface(name, formalParameters, returnValue); 
		Expect(7);
	}



	public void Parse(Source source) {
		this.scanner = new Scanner(source.getInputStream());
		la = new Token();
		la.val = "";		
		Get();
		Pascal();
		Expect(0);

	}

	private static final boolean[][] set = {
		{_T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x},
		{_x,_x,_x,_x, _x,_T,_x,_x, _T,_x,_x,_x, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _T,_x,_x,_x, _x,_x,_x,_x, _T,_T,_x,_x, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x},
		{_x,_x,_x,_x, _x,_x,_x,_x, _T,_x,_x,_x, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _T,_x,_x,_x, _x,_x,_x,_x, _T,_T,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x},
		{_x,_T,_x,_T, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _T,_T,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x},
		{_x,_T,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_T, _x,_x,_x,_x, _x,_x,_T,_T, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x},
		{_x,_x,_x,_x, _x,_x,_x,_x, _T,_x,_x,_x, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _T,_x,_x,_x, _x,_x,_x,_x, _T,_T,_x,_x, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x},
		{_x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_T,_T, _T,_T,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x},
		{_x,_T,_T,_T, _T,_x,_x,_T, _x,_x,_T,_x, _x,_x,_x,_T, _T,_T,_x,_x, _x,_x,_T,_T, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _T,_T,_T,_T, _T,_T,_x,_T, _x,_x,_x,_x, _T,_x,_T,_T, _x,_x,_x,_x, _x,_x,_T,_x, _x,_x,_x,_x},
		{_x,_x,_x,_x, _x,_x,_x,_T, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_T,_x,_x, _x,_x,_T,_x, _x,_x,_x,_x, _x,_T,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x},
		{_x,_T,_T,_T, _T,_x,_x,_x, _x,_x,_T,_x, _x,_x,_x,_x, _T,_T,_x,_x, _x,_x,_T,_T, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_T,_x, _x,_x,_x,_x},
		{_x,_x,_x,_x, _x,_x,_x,_x, _x,_T,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_T,_T,_T, _T,_T,_x,_x, _x,_x,_x,_x},
		{_x,_T,_T,_T, _T,_x,_x,_x, _x,_x,_T,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_T,_T, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_T,_x, _x,_x,_x,_x},
		{_x,_x,_x,_x, _x,_x,_T,_T, _x,_T,_x,_T, _x,_T,_T,_x, _T,_T,_T,_T, _T,_T,_x,_x, _x,_T,_T,_x, _x,_x,_T,_x, _x,_x,_x,_x, _x,_T,_x,_x, _x,_x,_T,_x, _x,_T,_T,_T, _x,_T,_x,_x, _T,_T,_T,_T, _T,_T,_x,_x, _x,_x,_x,_x},
		{_x,_T,_T,_T, _T,_x,_x,_x, _x,_x,_T,_x, _x,_x,_x,_T, _T,_T,_x,_x, _x,_x,_T,_T, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_T,_x, _x,_x,_x,_x},
		{_x,_x,_x,_x, _x,_x,_x,_x, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _T,_x,_x,_x, _x,_x,_x,_x, _T,_T,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x}

	};
	
    public boolean noErrors(){
    	return errors.count == 0;
    }
    
    public boolean caseEnds(){
    	if(la.val.toLowerCase().equals("end") && !t.val.toLowerCase().equals(":"))
    		return true;
    		
    	if(la.val.toLowerCase().equals("else"))
    		return true;
    		
    	else if(la.val.toLowerCase().equals(";")){
    		Token next = scanner.Peek();
    		return next.val.toLowerCase().equals("end") || next.val.toLowerCase().equals("else");
    	}
    	
    	return false;
    }
    
    public boolean isSingleIdentifier(){
    	Token next = scanner.Peek();
    	if(next.val.equals("]") && factory.containsIdentifier(la.val.toLowerCase()))
    		return true;

    	return false;
    }
    
    public boolean continuesArray(){
    	Token next = scanner.Peek();
    	if(next.val.toLowerCase().equals("array") || (next.val.toLowerCase().equals("packed")))
    		return true;

    	return false;
    }
} // end Parser

class NumericConstant { 
	private Object value;
	public boolean isDoubleType;
	
	public NumericConstant(Object value, boolean isDoubleType) {
		this.value = value;
		this.isDoubleType = isDoubleType;
	}
	
	public NumericConstant(Object value) {
		this.value = value;
		this.isDoubleType = (value instanceof Double);
	}
	
	public double getDouble() {
		return (isDoubleType)? (double)value : (double)(long)value;
	}
	
	public long getLong() {
		assert !isDoubleType;
		
		return (long)value;
	}
}

class FormalParameter {
	public FormalParameter(String id, String type, boolean isOutput){
		this.type = type;
		this.identifier = id;
		this.isOutput = isOutput;
	}
	
	public String type;
	public String identifier;
	public boolean isOutput;
}

class Errors {
	public int count = 0;                                    // number of errors detected
	public java.io.PrintStream errorStream = System.err;     // error messages go to this stream
	public String errMsgFormat = "-- line {0} col {1}: {2}"; // 0=line, 1=column, 2=text
	
	protected void printMsg(int line, int column, String msg) {
		StringBuffer b = new StringBuffer(errMsgFormat);
		int pos = b.indexOf("{0}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, line); }
		pos = b.indexOf("{1}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, column); }
		pos = b.indexOf("{2}");
		if (pos >= 0) b.replace(pos, pos+3, msg);
		errorStream.println(b.toString());
	}
	
	public void SynErr (int line, int col, int n) {
		String s;
		switch (n) {
			case 0: s = "EOF expected"; break;
			case 1: s = "identifier expected"; break;
			case 2: s = "stringLiteral expected"; break;
			case 3: s = "numericLiteral expected"; break;
			case 4: s = "doubleLiteral expected"; break;
			case 5: s = "\"uses\" expected"; break;
			case 6: s = "\",\" expected"; break;
			case 7: s = "\";\" expected"; break;
			case 8: s = "\"type\" expected"; break;
			case 9: s = "\"=\" expected"; break;
			case 10: s = "\"(\" expected"; break;
			case 11: s = "\")\" expected"; break;
			case 12: s = "\"const\" expected"; break;
			case 13: s = "\"or\" expected"; break;
			case 14: s = "\"and\" expected"; break;
			case 15: s = "\"not\" expected"; break;
			case 16: s = "\"+\" expected"; break;
			case 17: s = "\"-\" expected"; break;
			case 18: s = "\"*\" expected"; break;
			case 19: s = "\"/\" expected"; break;
			case 20: s = "\"div\" expected"; break;
			case 21: s = "\"mod\" expected"; break;
			case 22: s = "\"true\" expected"; break;
			case 23: s = "\"false\" expected"; break;
			case 24: s = "\"var\" expected"; break;
			case 25: s = "\":\" expected"; break;
			case 26: s = "\"of\" expected"; break;
			case 27: s = "\"packed\" expected"; break;
			case 28: s = "\"array\" expected"; break;
			case 29: s = "\"[\" expected"; break;
			case 30: s = "\"]\" expected"; break;
			case 31: s = "\"..\" expected"; break;
			case 32: s = "\"function\" expected"; break;
			case 33: s = "\"procedure\" expected"; break;
			case 34: s = "\"forward;\" expected"; break;
			case 35: s = "\".\" expected"; break;
			case 36: s = "\"begin\" expected"; break;
			case 37: s = "\"end\" expected"; break;
			case 38: s = "\"break\" expected"; break;
			case 39: s = "\"randomize\" expected"; break;
			case 40: s = "\"readln\" expected"; break;
			case 41: s = "\"case\" expected"; break;
			case 42: s = "\"else\" expected"; break;
			case 43: s = "\"for\" expected"; break;
			case 44: s = "\":=\" expected"; break;
			case 45: s = "\"to\" expected"; break;
			case 46: s = "\"downto\" expected"; break;
			case 47: s = "\"do\" expected"; break;
			case 48: s = "\"repeat\" expected"; break;
			case 49: s = "\"until\" expected"; break;
			case 50: s = "\"while\" expected"; break;
			case 51: s = "\"if\" expected"; break;
			case 52: s = "\"then\" expected"; break;
			case 53: s = "\">\" expected"; break;
			case 54: s = "\">=\" expected"; break;
			case 55: s = "\"<\" expected"; break;
			case 56: s = "\"<=\" expected"; break;
			case 57: s = "\"<>\" expected"; break;
			case 58: s = "\"random\" expected"; break;
			case 59: s = "\"unit\" expected"; break;
			case 60: s = "\"interface\" expected"; break;
			case 61: s = "\"implementation\" expected"; break;
			case 62: s = "??? expected"; break;
			case 63: s = "invalid Pascal"; break;
			case 64: s = "invalid Declaration"; break;
			case 65: s = "invalid ConstantDefinition"; break;
			case 66: s = "invalid Subroutine"; break;
			case 67: s = "invalid Subroutine"; break;
			case 68: s = "invalid NumericConstant"; break;
			case 69: s = "invalid LogicFactorConstant"; break;
			case 70: s = "invalid LogicSingleConstant"; break;
			case 71: s = "invalid LogicLiteral"; break;
			case 72: s = "invalid StringValue"; break;
			case 73: s = "invalid NumericFactorConstant"; break;
			case 74: s = "invalid VariableLineDefinition"; break;
			case 75: s = "invalid Ordinal"; break;
			case 76: s = "invalid Statement"; break;
			case 77: s = "invalid ForLoop"; break;
			case 78: s = "invalid ReadStatement"; break;
			case 79: s = "invalid ReadStatement"; break;
			case 80: s = "invalid SignedLogicFactor"; break;
			case 81: s = "invalid SignedFactor"; break;
			case 82: s = "invalid Factor"; break;
			case 83: s = "invalid Factor"; break;
			case 84: s = "invalid Random"; break;
			case 85: s = "invalid Random"; break;
			case 86: s = "invalid MemberExpression"; break;
			case 87: s = "invalid ArrayAccessing"; break;
			case 88: s = "invalid ArrayIndex"; break;
			default: s = "error " + n; break;
		}
		printMsg(line, col, s);
		count++;
	}

	public void SemErr (int line, int col, String s) {	
		printMsg(line, col, s);
		count++;
	}
	
	public void SemErr (String s) {
		errorStream.println(s);
		count++;
	}
	
	public void Warning (int line, int col, String s) {	
		printMsg(line, col, s);
	}
	
	public void Warning (String s) {
		errorStream.println(s);
	}
} // Errors


class FatalError extends RuntimeException {
	public static final long serialVersionUID = 1L;
	public FatalError(String s) { super(s); }
}
