package org.macrobit.autocollapsegenerics

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.editor.FoldingModel
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil

class AutoCollapseGenericsStartupActivity : /*StartupActivity,*/ ProjectActivity {

//    override fun runActivity(project: Project) {
//        collapseGenericsInEditor(project)
//    }

    private fun collapseGenericsInEditor(project: Project) {
        EditorFactory.getInstance().addEditorFactoryListener(object : EditorFactoryListener {
            override fun editorCreated(event: EditorFactoryEvent) {
                val editor: Editor = event.editor
                val document = editor.document
                val virtualFile = FileDocumentManager.getInstance().getFile(document) ?: return

                val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return
                if (psiFile !is PsiJavaFile) return

                collapseGenericsInEditor(editor, psiFile)
            }
        }, project)
    }

    override suspend fun execute(project: Project) {
        collapseGenericsInEditor(project)
    }

    private fun collapseGenericsInEditor(editor: Editor, psiFile: PsiJavaFile) {
        val foldingModel: FoldingModel = editor.foldingModel
        val classes: List<PsiClass> = PsiTreeUtil.getChildrenOfTypeAsList(psiFile, PsiClass::class.java)

        foldingModel.runBatchFoldingOperation {
            for (psiClass in classes) {
                // Fold class type parameters
                psiClass.typeParameterList?.let { tpl ->
                    addFoldRegionIfPossible(foldingModel, tpl.textRange.startOffset, tpl.textRange.endOffset)
                }

                // Fold generics in 'extends'
                psiClass.extendsList?.referenceElements?.forEach { ref ->
                    ref.parameterList?.let { pl ->
                        addFoldRegionIfPossible(foldingModel, pl.textRange.startOffset, pl.textRange.endOffset)
                    }
                }

                // Fold generics in 'implements'
                psiClass.implementsList?.referenceElements?.forEach { ref ->
                    ref.parameterList?.let { pl ->
                        addFoldRegionIfPossible(foldingModel, pl.textRange.startOffset, pl.textRange.endOffset)
                    }
                }

                // Fold generics in method (incl. constructor) parameters
                psiClass.methods.forEach { method ->
                    method.parameterList.parameters.forEach { param ->
                        param.typeElement?.innermostComponentReferenceElement?.parameterList?.let { pl ->
                            addFoldRegionIfPossible(foldingModel, pl.textRange.startOffset, pl.textRange.endOffset)
                        }
                    }
                }

                // Fold generics in fields
                psiClass.fields.forEach { field ->
                    val typeElement = field.typeElement
                    typeElement?.children?.forEach { child ->
                        if (child is PsiJavaCodeReferenceElement) {
                            child.parameterList?.let { pl ->
                                addFoldRegionIfPossible(foldingModel, pl.textRange.startOffset, pl.textRange.endOffset)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun addFoldRegionIfPossible(foldingModel: FoldingModel, start: Int, end: Int) {
        if (end - start < 3) return
        val region: FoldRegion? = foldingModel.addFoldRegion(start, end, "<...>")
        region?.isExpanded = false
    }

}
