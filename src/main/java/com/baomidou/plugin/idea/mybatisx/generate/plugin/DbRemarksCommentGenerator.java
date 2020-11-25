package com.baomidou.plugin.idea.mybatisx.generate.plugin;

import com.baomidou.plugin.idea.mybatisx.util.StringUtils;
import org.mybatis.generator.api.CommentGenerator;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.dom.java.CompilationUnit;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.InnerClass;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.internal.DefaultCommentGenerator;
import org.mybatis.generator.internal.util.StringUtility;

import java.util.Properties;

/**
 * @author ls9527
 */

public class DbRemarksCommentGenerator extends DefaultCommentGenerator implements CommentGenerator {
    private Properties properties = new Properties();
    private boolean isAnnotations;

    public DbRemarksCommentGenerator() {
    }

    @Override
    public void addJavaFileComment(CompilationUnit compilationUnit) {

    }

    @Override
    public void addConfigurationProperties(Properties properties) {
        this.properties.putAll(properties);
        this.isAnnotations = StringUtility.isTrue(properties.getProperty("annotations"));
    }

    @Override
    public void addClassComment(InnerClass innerClass, IntrospectedTable introspectedTable) {
    }

    @Override
    public void addModelClassComment(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        topLevelClass.addJavaDocLine("/**");
        topLevelClass.addJavaDocLine(" * @TableName " + introspectedTable.getFullyQualifiedTable().getIntrospectedTableName());
        topLevelClass.addJavaDocLine(" */");
        if (this.isAnnotations) {
            topLevelClass.addImportedType(new FullyQualifiedJavaType("javax.persistence.Table"));
            topLevelClass.addImportedType(new FullyQualifiedJavaType("javax.persistence.Id"));
            topLevelClass.addImportedType(new FullyQualifiedJavaType("javax.persistence.Column"));
            topLevelClass.addImportedType(new FullyQualifiedJavaType("javax.persistence.GeneratedValue"));
            topLevelClass.addAnnotation("@Table(name=\"" + introspectedTable.getFullyQualifiedTableNameAtRuntime() + "\")");
        }

    }


    @Override
    public void addGetterComment(Method method, IntrospectedTable introspectedTable, IntrospectedColumn introspectedColumn) {
        method.addJavaDocLine("/**");
        if (!StringUtils.isEmpty(introspectedColumn.getRemarks())) {
            method.addJavaDocLine(" * " + introspectedColumn.getRemarks());
        }
        this.addJavadocTag(method, false);
        method.addJavaDocLine(" */");
    }

    @Override
    public void addSetterComment(Method method, IntrospectedTable introspectedTable, IntrospectedColumn introspectedColumn) {
        method.addJavaDocLine("/**");
        if (!StringUtils.isEmpty(introspectedColumn.getRemarks())) {
            method.addJavaDocLine(" * " + introspectedColumn.getRemarks());
        }
        this.addJavadocTag(method, false);
        method.addJavaDocLine(" */");
    }

    @Override
    public void addFieldComment(Field field, IntrospectedTable introspectedTable, IntrospectedColumn introspectedColumn) {
        field.addJavaDocLine("/**");
        StringBuilder sb = new StringBuilder();
        sb.append(" * ");
        if (!StringUtils.isEmpty(introspectedColumn.getRemarks())) {
            sb.append(introspectedColumn.getRemarks());
        }
        field.addJavaDocLine(sb.toString());
        this.addJavadocTag(field, false);
        field.addJavaDocLine(" */");

        if (this.isAnnotations) {
            for (IntrospectedColumn column : introspectedTable.getPrimaryKeyColumns()) {
                if (introspectedColumn == column) {
                    field.addAnnotation("@Id");
                    break;
                }
            }
            if (introspectedColumn.isIdentity()) {
                if (introspectedTable.getTableConfiguration().getGeneratedKey().getRuntimeSqlStatement().equals("JDBC")) {
                    field.addAnnotation("@GeneratedValue(generator = \"JDBC\")");
                } else {
                    field.addAnnotation("@GeneratedValue(strategy = GenerationType.IDENTITY)");
                }
            } else if (introspectedColumn.isSequenceColumn()) {
                field.addAnnotation("@SequenceGenerator(name=\"\",sequenceName=\"" + introspectedTable.getTableConfiguration().getGeneratedKey().getRuntimeSqlStatement() + "\")");
            }

            String column = introspectedColumn.getActualColumnName();
            if (StringUtility.stringContainsSpace(column) || introspectedTable.getTableConfiguration().isAllColumnDelimitingEnabled()) {
                column = introspectedColumn.getContext().getBeginningDelimiter()
                    + column
                    + introspectedColumn.getContext().getEndingDelimiter();
            }
            if (!column.equals(introspectedColumn.getJavaProperty())) {
                //@Column
                field.addAnnotation("@Column(name = \"" + column + "\")");
            }
        }

    }

    @Override
    public void addClassComment(InnerClass innerClass, IntrospectedTable introspectedTable, boolean markAsDoNotDelete) {
        innerClass.addJavaDocLine("/**");
        innerClass.addJavaDocLine(" */");
    }
}