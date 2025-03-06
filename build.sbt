ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / description := "Another Lexer Parser And Compiler Alpaca"

ThisBuild / scalaVersion := "3.6.3"

lazy val root = (project in file("."))
  .settings(
    name := "alpaca",
    idePackagePrefix := Some("com.alpaca")
  )
