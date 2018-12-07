package Action;

import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.NonEmptyInputValidator;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

public class AutoLog extends EditorAction {

   protected AutoLog(){
       super(new AutoLog.Handler());
   }

    public static class Handler extends EditorWriteActionHandler {
        public Handler() {
        }

        public void executeWriteAction(final Editor var1, DataContext var2) {
            if (!var1.getSelectionModel().hasSelection(true)) {
                if (Registry.is("editor.skip.copy.and.cut.for.empty.selection")) {
                    return;
                }


                //若没有选中内容，则默认选中光标停留的那一行
                var1.getCaretModel().runForEachCaret(new CaretAction() {
                    public void perform(Caret var1x) {
                        var1.getSelectionModel().selectWordAtCaret(false);
                    }
                });
            }

            SelectionModel sm = var1.getSelectionModel();
            String txt = sm.getSelectedText(); //获取选中的内容
            String txtTrim = txt.replaceAll("\n","").trim();
            StringBuffer contentBuffer = new StringBuffer();
            PsiFile psiFile = var2.getData(LangDataKeys.PSI_FILE);
            if (psiFile != null) {
                int offSet = var1.getCaretModel().getOffset();
                PsiElement elementAt = psiFile.findElementAt(offSet);
                PsiClass psiClass = PsiTreeUtil.getParentOfType(elementAt,PsiClass.class);
                if(null!=psiClass){
                    contentBuffer.append(psiClass.getName()!=null?psiClass.getName():"unNamed class");
                    contentBuffer.append(":");
                }
                PsiMethod method = PsiTreeUtil.getParentOfType(elementAt, PsiMethod.class);
                if(null!=method){
                    contentBuffer.append(method.getName());
                    PsiParameterList psiParameterList = method.getParameterList();
                    PsiParameter[] psiArr = psiParameterList.getParameters();
                    if(null!=psiArr && psiArr.length>0){
                        contentBuffer.append("(\"+");
                        for(PsiParameter psiParameter:psiArr){
                            contentBuffer.append(psiParameter.getName());
                            contentBuffer.append("+\",\"+");
                        }
                        contentBuffer.delete(contentBuffer.length()-5,contentBuffer.length());
                        contentBuffer.append("+\")");
                    }
                    contentBuffer.append(":");
                }
            }
            /*
            and line count
             */
            CaretModel caretModel = var1.getCaretModel();
            LogicalPosition logicalPosition = caretModel.getLogicalPosition();
            int lineOffset = logicalPosition.column - txtTrim.length();
            if(null!=logicalPosition){
                contentBuffer.append(logicalPosition.line+1);
            }
            String showS = dealTimeString(txt,contentBuffer,lineOffset);
            if(null == showS){
                showS = dealTraceString(txt,contentBuffer);
            }
            if(null == showS) {
                showS= "Log.d(\""+txtTrim + "\",\""+contentBuffer.toString()+"\");\n";
            }
            if (showS != null && !"".equals(showS.trim())) {
                EditorModificationUtil.insertStringAtCaret(var1, showS, true, false);
            }
        }
    }
    private static String dealTimeString(String tag,StringBuffer buffer,int lineOffset){
        if(null==tag || "".equals(tag.trim())){
            return null;
        }
        if(!tag.toLowerCase().endsWith("_time")){
            return null;
        }

        String lineOffsetS = "";
        if(lineOffset>0){
            StringBuffer tmpBuffer = new StringBuffer();
            for(int i=0;i<lineOffset;i++){
                tmpBuffer.append(" ");
            }
            lineOffsetS = tmpBuffer.toString();
        }
        String realTag = tag.substring(0,tag.toLowerCase().indexOf("_time"));
        String timeStringS =  "long start = System.currentTimeMillis();\n\n";
        String costStringS = lineOffsetS+"long cost = System.currentTimeMillis()-start;\n";
        String tmpResult = lineOffsetS+"Log.d(\""+realTag + "\",\""+buffer.toString()+":costTime: \"+cost);\n";
        return new StringBuffer().append(timeStringS).append(costStringS).append(tmpResult).toString();
    }

    private static String dealTraceString(String tag,StringBuffer buffer){
        if(null==tag || "".equals(tag.trim())){
            return null;
        }
        if(!tag.toLowerCase().endsWith("_trace")){
            return null;
        }

        String realTag = tag.substring(0,tag.toLowerCase().indexOf("_trace"));
        String traceS = "android.util.Log.getStackTraceString(new Throwable())";
        String result = "Log.d(\""+realTag + "\",\""+buffer.toString()+":trace: \"+"+traceS+");\n";
        return result;
    }
}
