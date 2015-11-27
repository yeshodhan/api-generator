package com.mickoo.api;

import com.sun.codemodel.*;

import java.util.HashSet;
import java.util.Set;

/**
 * com.mickoo.xml.xsd2simplexml
 *
 * @author Yeshodhan Kulkarni (yeshodhan.kulkarni@gmail.com)
 * @version 1.0
 * @since 1.0
 */
public class GeneratedClass {

    CodeGenerator codeGenerator;
    JCodeModel codeModel;
    JDefinedClass generatedInterface;
    JDefinedClass generatedImpl;
    GeneratedClass generatedSuperInterface;

    Set<String> capabilities = new HashSet<String>();

    public GeneratedClass(CodeGenerator codeGenerator, JDefinedClass generatedInterface, JDefinedClass generatedImpl, GeneratedClass generatedSuperInterface) {
        this.codeGenerator = codeGenerator;
        this.codeModel = codeGenerator.codeModel;
        this.generatedInterface = generatedInterface;
        this.generatedImpl = generatedImpl;
        this.generatedSuperInterface = generatedSuperInterface;
    }

    public void addCapability(String capabilityName, GeneratedClass requestWrapper, GeneratedClass responseHandler, GeneratedClass baseRequest, GeneratedClass baseResponse) throws JClassAlreadyExistsException {
        if(capabilities.contains(capabilityName)) return;

        createCapabilityConstant(capabilityName);

        createCapabilityMethod(capabilityName, requestWrapper, responseHandler, baseRequest, baseResponse);

        capabilities.add(capabilityName);
    }

    public void createCapabilityConstant(String capabilityName) {
        generatedImpl.field(JMod.PUBLIC | JMod.STATIC | JMod.FINAL, String.class, capabilityName, JExpr.lit(capabilityName));
    }

    public void createCapabilityMethod(String capabilityName, GeneratedClass requestWrapper, GeneratedClass responseHandler, GeneratedClass baseRequest, GeneratedClass baseResponse) throws JClassAlreadyExistsException {

        String requestClassName = NameConverter.smart.toClassName(capabilityName) + "Request";
        String responseClassName = NameConverter.smart.toClassName(capabilityName) + "Response";

        String qualifiedRequestClassName = generatedInterface.getPackage().name() + ".impl." + requestClassName;
        String qualifiedResponseClassName = generatedInterface.getPackage().name() + ".impl." + responseClassName;

        GeneratedClass requestClass = codeGenerator.createClass(qualifiedRequestClassName, baseRequest);
        GeneratedClass responseClass = codeGenerator.createClass(qualifiedResponseClassName, baseResponse);


        JMethod jGetMethod = requestClass.generatedImpl.method(JMod.PUBLIC, String.class, "getMethod");
        jGetMethod.body()
                ._return(generatedImpl.staticRef(capabilityName));


        JClass jRequestClass =requestWrapper.generatedImpl.narrow(requestClass.generatedImpl);

        String requestParamName = NameConverter.smart.toVariableName(requestClassName);

        JMethod capability = generatedInterface.method(JMod.PUBLIC, codeModel.VOID, capabilityName);
        capability.param(jRequestClass, requestParamName);
        capability.param(responseHandler.generatedImpl, "handler");

        JMethod capabilityImpl = generatedImpl.method(JMod.PUBLIC, codeModel.VOID, capabilityName);
        capabilityImpl.annotate(Override.class);
        capabilityImpl.param(jRequestClass, requestParamName);
        capabilityImpl.param(responseHandler.generatedImpl, "handler");
        capabilityImpl.body().directStatement("notSupported("+capabilityName+", handler);");



        generatedImpl.getMethod("init", new JType[]{}).body()
                .invoke(JExpr.ref("requestClzMap"), "put")
                .arg(JExpr.ref(capabilityName))
                .arg(JExpr.dotclass(requestClass.generatedImpl));
        generatedImpl.getMethod("init", new JType[]{}).body()
                .invoke(JExpr.ref("responseClzMap"), "put")
                .arg(JExpr.ref(capabilityName))
                .arg(JExpr.dotclass(responseClass.generatedImpl));



    }

}
