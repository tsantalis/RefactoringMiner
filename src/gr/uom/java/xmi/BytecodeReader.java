package gr.uom.java.xmi;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.util.TraceSignatureVisitor;

public class BytecodeReader {
	private UMLModel umlModel;

	public BytecodeReader(File rootFile) {
		this.umlModel = new UMLModel();
		recurse(rootFile);
	}

	public UMLModel getUmlModel() {
		return umlModel;
	}

	private void recurse(File rootFile) {
		if(rootFile.isDirectory()) {
			File[] files = rootFile.listFiles();
			for(File file : files) {
				if(file.isDirectory())
					recurse(file);
				else {
					String fileName = file.getName();
					if(fileName.contains(".")) {
						String extension = fileName.substring(fileName.lastIndexOf("."));
						if(extension.equalsIgnoreCase(".class")) {
							parseBytecode(file);
						}
					}
				}
			}
		}
		else {
			String fileName = rootFile.getName();
			if(fileName.contains(".")) {
				String extension = fileName.substring(fileName.lastIndexOf("."));
				if(extension.equalsIgnoreCase(".class")) {
					parseBytecode(rootFile);
				}
			}
		}
	}

	private void parseBytecode(File classFile) {
		try {
			FileInputStream fin = new FileInputStream(classFile);
			ClassReader cr = new ClassReader(new DataInputStream(fin));
			ClassNode cn = new ClassNode();
			cr.accept(cn, ClassReader.SKIP_DEBUG);

			String qualifiedClassName = cn.name.replaceAll("/", ".").replaceAll("\\$", ".");
			String packageName = null;
			String className = null;
			if(qualifiedClassName.contains(".")) {
				packageName = qualifiedClassName.substring(0, qualifiedClassName.lastIndexOf("."));
				className = qualifiedClassName.substring(qualifiedClassName.lastIndexOf(".")+1);
			}
			else {
				packageName = "";
				className = qualifiedClassName;
			}

			UMLClass umlClass = new UMLClass(packageName, className, null);
			if((cn.access & Opcodes.ACC_INTERFACE) != 0)
				umlClass.setInterface(true);
			else if((cn.access & Opcodes.ACC_ABSTRACT) != 0)
				umlClass.setAbstract(true);

			if((cn.access & Opcodes.ACC_PUBLIC) != 0)
				umlClass.setVisibility("public");
			else if((cn.access & Opcodes.ACC_PROTECTED) != 0)
				umlClass.setVisibility("protected");
			else if((cn.access & Opcodes.ACC_PRIVATE) != 0)
				umlClass.setVisibility("private");
			else
				umlClass.setVisibility("package");

			//if ((cn.access & Opcodes.ACC_STATIC) != 0)
			//	umlClass.setStatic(true);
			String superClassType = cn.superName;
			if(!superClassType.equals("java/lang/Object")) {
				String type = superClassType.replaceAll("/", ".").replaceAll("\\$", ".");
				UMLType umlType = UMLType.extractTypeObject(type);
				UMLGeneralization umlGeneralization = new UMLGeneralization(umlClass.getName(), umlType.getClassType());
				umlClass.setSuperclass(umlType);
				umlModel.addGeneralization(umlGeneralization);
			}

			List<String> superInterfaceTypes = cn.interfaces;
			for(String interfaceType : superInterfaceTypes) {
				UMLRealization umlRealization = new UMLRealization(umlClass.getName(), interfaceType.replaceAll("/", ".").replaceAll("\\$", "."));
				umlModel.addRealization(umlRealization);
			}

			List<UMLAttribute> staticFinalFields = new ArrayList<UMLAttribute>();
			List<FieldNode> fields = cn.fields;
			for(FieldNode fieldNode : fields) {
				UMLAttribute umlAttribute = null;
				if(fieldNode.signature != null) {
					TraceSignatureVisitor v = new TraceSignatureVisitor(ClassReader.SKIP_DEBUG);
					SignatureReader r = new SignatureReader(fieldNode.signature);
					r.accept(v);
					String declaration = v.getDeclaration();
					if(declaration.startsWith(" extends "))
						declaration = declaration.substring(9, declaration.length());
					UMLType type = UMLType.extractTypeObject(declaration);
					umlAttribute = new UMLAttribute(fieldNode.name, type);
				}
				else {
					Type fieldType = Type.getType(fieldNode.desc);
					UMLType type = UMLType.extractTypeObject(fieldType.getClassName().replaceAll("\\$", "."));
					umlAttribute = new UMLAttribute(fieldNode.name, type);
				}
				umlAttribute.setClassName(umlClass.getName());

				if((fieldNode.access & Opcodes.ACC_PUBLIC) != 0)
					umlAttribute.setVisibility("public");
				else if((fieldNode.access & Opcodes.ACC_PROTECTED) != 0)
					umlAttribute.setVisibility("protected");
				else if((fieldNode.access & Opcodes.ACC_PRIVATE) != 0)
					umlAttribute.setVisibility("private");
				else
					umlAttribute.setVisibility("package");

				if ((fieldNode.access & Opcodes.ACC_FINAL) != 0)
					umlAttribute.setFinal(true);
				
				if ((fieldNode.access & Opcodes.ACC_STATIC) != 0)
					umlAttribute.setStatic(true);
				
				if(umlAttribute.isFinal() && umlAttribute.isStatic()) {
					umlAttribute.setValue(fieldNode.value);
					staticFinalFields.add(umlAttribute);
				}
				umlClass.addAttribute(umlAttribute);
			}

			List<MethodNode> methods = cn.methods;
			for(MethodNode methodNode : methods) {
				String methodName = null;
				if(methodNode.name.equals("<init>"))
					methodName = className;
				else
					methodName = methodNode.name;
				UMLOperation umlOperation = new UMLOperation(methodName, null);
				umlOperation.setClassName(umlClass.getName());
				if(methodNode.name.equals("<init>"))
					umlOperation.setConstructor(true);

				if ((methodNode.access & Opcodes.ACC_PUBLIC) != 0)
					umlOperation.setVisibility("public");
				else if ((methodNode.access & Opcodes.ACC_PROTECTED) != 0)
					umlOperation.setVisibility("protected");
				else if ((methodNode.access & Opcodes.ACC_PRIVATE) != 0)
					umlOperation.setVisibility("private");
				else
					umlOperation.setVisibility("package");

				if(!methodNode.name.equals("<init>")) {
					if ((methodNode.access & Opcodes.ACC_ABSTRACT) != 0)
						umlOperation.setAbstract(true);
					if ((methodNode.access & Opcodes.ACC_FINAL) != 0)
						umlOperation.setFinal(true);
					if ((methodNode.access & Opcodes.ACC_STATIC) != 0)
						umlOperation.setStatic(true);
				}

				if(methodNode.signature != null) {
					TraceSignatureVisitor v = new TraceSignatureVisitor(ClassReader.SKIP_DEBUG);
					SignatureReader r = new SignatureReader(methodNode.signature);
					r.accept(v);
					if(!methodNode.name.equals("<init>")) {
						UMLType rtype = UMLType.extractTypeObject(v.getReturnType());
						UMLParameter returnParameter = new UMLParameter("return", rtype, "return");
						umlOperation.addParameter(returnParameter);
					}
					
					String declaration = v.getDeclaration();
					String parameterTypes = declaration.substring(declaration.indexOf("(")+1, declaration.lastIndexOf(")"));
					if(!parameterTypes.isEmpty()) {
						String[] tokens = parameterTypes.split(", ");
						for(String token : tokens) {
							UMLType type = UMLType.extractTypeObject(token);
							UMLParameter umlParameter = new UMLParameter("", type, "in");
							umlOperation.addParameter(umlParameter);
						}
					}
				}
				else {
					if(!methodNode.name.equals("<init>")) {
						Type returnType = Type.getReturnType(methodNode.desc);
						UMLType rtype = UMLType.extractTypeObject(returnType.getClassName().replaceAll("\\$", "."));
						UMLParameter returnParameter = new UMLParameter("return", rtype, "return");
						umlOperation.addParameter(returnParameter);
					}
					
					Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);
					for(Type argumentType : argumentTypes) {
						UMLType type = UMLType.extractTypeObject(argumentType.getClassName().replaceAll("\\$", "."));
						UMLParameter umlParameter = new UMLParameter("", type, "in");
						boolean skipArgument = false;
						if(cn.name.contains("$") && methodNode.name.equals("<init>") && argumentType.getClassName().replaceAll("\\$", ".").equals(packageName))
							skipArgument = true;
						if(!skipArgument)
							umlOperation.addParameter(umlParameter);
					}
				}

				if (methodNode.instructions.size() > 0) {
					Iterator instructionIterator = methodNode.instructions.iterator();
					while(instructionIterator.hasNext()) {
						AbstractInsnNode instruction = (AbstractInsnNode)instructionIterator.next();
						if(instruction instanceof FieldInsnNode) {
							FieldInsnNode fieldInstruction = (FieldInsnNode) instruction;
							Type fieldType = Type.getType(fieldInstruction.desc);
							FieldAccess fieldAccess = new FieldAccess(fieldInstruction.owner.replaceAll("/", ".").replaceAll("\\$", "."),
									fieldType.getClassName(), fieldInstruction.name);
							umlOperation.addAccessedMember(fieldAccess);
						}

						//special handling for accessed final static fields (constants)
						if(instruction instanceof LdcInsnNode) {
							LdcInsnNode ldcInstruction = (LdcInsnNode)instruction;
							Object value = ldcInstruction.cst;
							for(UMLAttribute attr : staticFinalFields) {
								Double attributeValue = null;
								Double fieldAccessValue = null;
								if(attr.getValue() instanceof Number) {
									attributeValue = ((Number)attr.getValue()).doubleValue();
								}
								if(value instanceof Number) {
									fieldAccessValue = ((Number)value).doubleValue();
								}
								if( (attr.getValue() != null && attr.getValue().equals(value)) ||
										(attributeValue != null && fieldAccessValue != null && attributeValue.equals(fieldAccessValue)) ) {
									FieldAccess fieldAccess = new FieldAccess(attr.getClassName(), attr.getType().getClassType(), attr.getName());
									umlOperation.addAccessedMember(fieldAccess);
								}
							}
						}

						if ((instruction.getOpcode() == Opcodes.INVOKEVIRTUAL) ||
								(instruction.getOpcode() == Opcodes.INVOKESTATIC) ||
								(instruction.getOpcode() == Opcodes.INVOKESPECIAL) ||
								(instruction.getOpcode() == Opcodes.INVOKEINTERFACE)) {

							MethodInsnNode methodInstruction = (MethodInsnNode) instruction;
							Type returnType = Type.getReturnType(methodInstruction.desc);
							UMLType rtype = UMLType.extractTypeObject(returnType.getClassName().replaceAll("\\$", "."));
							MethodCall methodCall = new MethodCall(
									methodInstruction.owner.replaceAll("/", ".").replaceAll("\\$", "."), methodInstruction.name,
									rtype);
							Type[] argTypes = Type.getArgumentTypes(methodInstruction.desc);
							for (Type argType : argTypes) {
								UMLType type = UMLType.extractTypeObject(argType.getClassName().replaceAll("\\$", "."));
								methodCall.addParameter(type);
							}
							boolean isObjectConstructorCall = false;
							if(methodInstruction.name.equals("<init>")) {
								methodCall.setConstructorCall(true);
								if(methodCall.getOriginClassName().equals("java.lang.Object"))
									isObjectConstructorCall = true;
							}
							
							if(!isObjectConstructorCall)
								umlOperation.addAccessedMember(methodCall);
						}

						if ((instruction.getOpcode() == Opcodes.NEW) ||
								(instruction.getOpcode() == Opcodes.ANEWARRAY)) {
							TypeInsnNode typeInstruction = (TypeInsnNode) instruction;
						}
					}
				}

				umlClass.addOperation(umlOperation);
			}

			umlModel.addClass(umlClass);
			fin.close();
		} catch(FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
