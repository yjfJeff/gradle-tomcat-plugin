package com.bmuschko.gradle.tomcat

import org.apache.commons.lang3.exception.ExceptionUtils
import com.bmuschko.gradle.tomcat.embedded.TomcatVersion
import org.gradle.tooling.BuildException
import spock.lang.Unroll

class TomcatJasperIntegrationTest extends AbstractIntegrationTest {
    def setup() {
        buildFile << """
apply plugin: com.bmuschko.gradle.tomcat.TomcatPlugin
"""
    }

    @Unroll
    def "Runs Jasper compiler for #tomcatVersion with default conventions"() {
        setup:
            setupWebAppDirectory()
            createJspFiles(new File(integTestDir, 'src/main/webapp'))

        expect:
            File outputDir = new File(integTestDir, 'build/jasper')
            buildFile << getBasicTomcatBuildFileContent(tomcatVersion)
            runTasks(integTestDir, TomcatPlugin.TOMCAT_JASPER_TASK_NAME)
            File compiledJspDir = new File(outputDir, 'org/apache/jsp')
            compiledJspDir.exists()
            new File(compiledJspDir, 'helloWorld_jsp.java').exists()
            new File(compiledJspDir, 'date_jsp.java').exists()

        where:
            tomcatVersion << [TomcatVersion.VERSION_6X, TomcatVersion.VERSION_7X, TomcatVersion.VERSION_8X]
    }

    /**
     * With Tomcat version 7.0.50 the Jasper compiler Ant task validation attribute was changed from "validateXml"
     * to "validateTld". The end user of the Gradle task will need to select one of these attributes depending
     * on the Tomcat version in use.
     */
    @Unroll
    def "Can use Jasper compiler validation for Tomcat version #tomcatVersion with attribute #validationAttribute"() {
        setup:
            setupWebAppDirectory()
            createJspFiles(new File(integTestDir, 'src/main/webapp'))

        expect:
            File outputDir = new File(integTestDir, 'build/jasper')
            buildFile << getBasicTomcatBuildFileContent(tomcatVersion)
            buildFile << """
tomcat {
    jasper {
        $validationAttribute = true
    }
}
"""
            runTasks(integTestDir, TomcatPlugin.TOMCAT_JASPER_TASK_NAME)
            File compiledJspDir = new File(outputDir, 'org/apache/jsp')
            compiledJspDir.exists()
            new File(compiledJspDir, 'helloWorld_jsp.java').exists()
            new File(compiledJspDir, 'date_jsp.java').exists()

        where:
            tomcatVersion  | validationAttribute
            '6.0.29'       | 'validateXml'
            '6.0.39'       | 'validateTld'
            '7.0.42'       | 'validateXml'
            '7.0.50'       | 'validateTld'
            '8.0.3'        | 'validateTld'
    }

    private void createJspFiles(File targetDir) {
        File helloWorldJspFile = new File(targetDir, 'helloWorld.jsp')
        helloWorldJspFile << """
<html>
    <body>
        <%= "Hello World!" %>
    </body>
</html>
"""

        File dateJspFile = new File(targetDir, 'date.jsp')
        dateJspFile << """
<%@ page language="java" import="java.util.*" errorPage="" %>
<html>
    <body>
        Current Date time: <%= new java.util.Date() %>
    </body>
</html>
"""
    }

    @Unroll
    def "Throws exception using Jasper compiler validation for Tomcat version #tomcatVersion with invalid attribute #validationAttribute"() {
        setup:
            setupWebAppDirectory()

        when:
            buildFile << getBasicTomcatBuildFileContent(tomcatVersion)
            buildFile << """
tomcat {
    jasper {
        $validationAttribute = true
    }
}
"""
            runTasks(integTestDir, TomcatPlugin.TOMCAT_JASPER_TASK_NAME)

        then:
            Throwable t = thrown(BuildException)
            Throwable rootCause = ExceptionUtils.getRootCause(t)
            rootCause.message == "The <jasper> type doesn't support the \"$validationAttribute\" attribute."

        where:
            tomcatVersion << ['6.0.39', '7.0.42', '7.0.50', '8.0.3']
            validationAttribute << ['validateXml', 'validateTld', 'validateXml', 'validateXml']
    }
}
