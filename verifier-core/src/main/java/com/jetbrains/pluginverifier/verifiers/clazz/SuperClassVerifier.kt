package com.jetbrains.pluginverifier.verifiers.clazz

import com.jetbrains.pluginverifier.results.problems.SuperClassBecameInterfaceProblem
import com.jetbrains.pluginverifier.verifiers.*
import org.objectweb.asm.tree.ClassNode

/**
 * Check that superclass exists and is indeed a class (not interface).
 *
 * @author Dennis.Ushakov
 */
class SuperClassVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, ctx: VerificationContext) {
    val superClassName = clazz.superName ?: ClassParentsVisitor.JAVA_LANG_OBJECT
    val superNode = ctx.resolveClassOrProblem(superClassName, clazz, { ctx.fromClass(clazz) }) ?: return
    //If the class or interface named as the direct superclass of C is in fact an interface, loading throws an IncompatibleClassChangeError.
    if (superNode.isInterface()) {
      ctx.registerProblem(SuperClassBecameInterfaceProblem(ctx.fromClass(clazz), ctx.fromClass(superNode)))
    }
  }
}
