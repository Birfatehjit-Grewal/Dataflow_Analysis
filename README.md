# Dataflow Analysis
This exercise was done as a part of Simon Fraser University's CMPT 479 course. The template was provided and only the KittenChecker.java is a product of my work.

Building with Maven
==============================================
From the project directory, you can build and run all unit tests with:

        mvn package

You can just compile with:

        mvn compile

You can clean your build using:

        mvn clean

And you can just run all unit tests for the expected analysis using:

        mvn test

Running your analysis
==============================================
It can also be handy to explicitly run the analysis yourself, outside of any
unit testing framework. You can analyze classes in the source tree using:

        mvn exec:exec -DSOOT_TARGET=<package qualified class name>

For example:

        mvn exec:exec -DSOOT_TARGET=ca.sfu.cmpt745.ex06.examples.Test_01_Basic

