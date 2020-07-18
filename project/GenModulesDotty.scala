import java.io.{File, BufferedWriter, FileWriter}
import scala.io.Source

object GenModulesDotty {

  private def uncommentJsExport(line: String): String =
    if (line.trim.startsWith("//DOTTY-ONLY "))
      line.substring(line.indexOf("//DOTTY-ONLY ") + 13)
    else if (line.trim.startsWith("//DOTTY-ONLY "))
      line.substring(line.indexOf("//DOTTY-ONLY ") + 13)
    else
      line

  private def transformLine(line: String): String =
    uncommentJsExport(line)

  private def copyFile(sourceFile: File, destFile: File): File = {
    val destWriter = new BufferedWriter(new FileWriter(destFile))
    try {
      val lines = Source.fromFile(sourceFile).getLines.toList
      var skipMode = false
      for (line <- lines) {
        if (line.trim == "// SKIP-DOTTY-START" || line.trim == "// SKIP-DOTTY-START")
          skipMode = true
        else if (line.trim == "// SKIP-DOTTY-END" || line.trim == "// SKIP-DOTTY-END")
          skipMode = false
        else if (!skipMode) {
          destWriter.write(transformLine(line))
          destWriter.newLine()
        }
      }
      destFile
    }
    finally {
      destWriter.flush()
      destWriter.close()
      println("Copied " + destFile.getAbsolutePath)
    }
  }

  def copyDir(sourceDirName: String, packageDirName: String, targetDir: File, skipList: List[String]): Seq[File] = {
    val packageDir = new File(targetDir, packageDirName)
    packageDir.mkdirs()
    val sourceDir = new File(sourceDirName)
    sourceDir.listFiles.toList.filter(f => f.isFile && !skipList.contains(f.getName) && (f.getName.endsWith(".scala") || f.getName.endsWith(".java"))).map { sourceFile =>
      val destFile = new File(packageDir, sourceFile.getName)
      if (!destFile.exists || sourceFile.lastModified > destFile.lastModified)
        copyFile(sourceFile, destFile)

      destFile
    }
  }
  
  /** (targetDir, version, scalaVersion) => generated files */
  type GenFn = (File, String, String) => Seq[File]
  
  def apply(moduleDirName: String, packagePaths: Seq[String]): GenFn = (targetDir, version, scalaVersion) => {
    GenScalaTestDotty.genScalaPackages
      .filter { case (packagePath, _) => packagePaths.contains(packagePath) }
      .flatMap { case (packagePath, skipList) =>
        copyDir(s"jvm/$moduleDirName/src/main/scala/" + packagePath, packagePath, targetDir, skipList)
      }.toList
  }
  
  def apply(style: String): GenFn = apply(style, Seq(s"org/scalatest/$style"))

  val genScalaTestCore: GenFn = apply(
    "core",
    Seq(
      "org/scalatest",
      "org/scalatest/compatible",
      "org/scalatest/concurrent",
      "org/scalatest/enablers",
      "org/scalatest/exceptions",
      "org/scalatest/events",
      "org/scalatest/fixture",
      "org/scalatest/prop",
      "org/scalatest/tagobjects",
      "org/scalatest/tags",
      "org/scalatest/time",
      "org/scalatest/tools",
      "org/scalatest/verbs",
    )
  )

  val genScalaTestMatchersCore: GenFn = apply(
    "matchers-core",
    Seq(
      "org/scalatest/matchers",
      "org/scalatest/matchers/dsl"
    )
  )

  val genScalaTestShouldMatchers: GenFn = apply("shouldmatchers", Seq("org/scalatest/matchers/should"))
}
