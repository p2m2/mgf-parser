package fr.inrae.p2m2.webapp

import fr.inrae.p2m2.format.MGFFeaturesIon
import fr.inrae.p2m2.parser.MGF
import fr.inrae.p2m2.webapp.GeneralStatisticsHtmlDivManagement.setGeneralStatistics
import org.scalajs.dom
import org.scalajs.dom.html.{Input, Progress}
import org.scalajs.dom.{FileReader, HTMLInputElement}
import scalatags.JsDom
import scalatags.JsDom.all._

import scala.annotation.unused
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

object MGFWebApp {
  def readFileAsText (file : dom.File) (implicit @unused ec: ExecutionContext) : Future[String] = {
    val p = Promise[String]()
    val fr = new FileReader()

    fr.onload = _ => {
      p.success(fr.result.toString)

    }

    fr.onerror = _ => {
      p.failure(new Exception())
    }

    fr.readAsText(file,"ISO-8859-1")
    p.future
  }

  private def setLog() : Unit = {
    val el = dom
      .document
      .getElementById("log")

    if (el != null) el.innerText = ""

  }
  def main(args: Array[String]): Unit = {

    val inputTag: JsDom.TypedTag[Input] = input(
      id := "inputFiles",
      `type` := "file",
      //multiple := "multiple",
      onchange := {
        (ev : dom.InputEvent) =>

          dom
            .document
            .getElementById("file").asInstanceOf[Progress].setAttribute("value", "0")

          val files = ev.currentTarget.asInstanceOf[HTMLInputElement].files

          if (files.nonEmpty) {
            setLog()

            val lFutures = Future.sequence(files.map(f => readFileAsText(f) ))
            lFutures.onComplete {
              case Success(reportsGcmsInTextFormat : List[String]) =>
                val listSourceFragment : List [String] = reportsGcmsInTextFormat.flatMap {
                  fileContent =>
                    val textByLine: Seq[String] = fileContent.split("\n")

                    val listFeatures: Seq[MGFFeaturesIon] = MGF.parse(textByLine,
                      (nLineRead:Int) => {
                        val percent = ((nLineRead.toDouble / textByLine.length.toDouble)*100).toInt
                        //System.err.println(percent)
                        dom
                          .document
                          .getElementById("file").asInstanceOf[Progress].setAttribute("value", percent.toString)
                    })

                    setGeneralStatistics(listFeatures)

                    /*
                    listFeatures.flatMap {
                      feature =>
                        CaptureIonFragmentSource.getFragmentSourcesFromFeature(feature, listFeatures).map {
                          x => s"${feature.id},${PropertyIon.retentionTime(feature)},${x.id},${PropertyIon.retentionTime(x)}"
                        }
                    }*/
                    listFeatures.map(_.id)
                }
                /*
                a(
                  "IsoCor file", href := "data:text/tsv;name=isocor_gcms.tsv;charset=ISO-8859-1,"
                    + encodeURIComponent(listSourceFragment.mkString("\n"))).render.click()*/

              case Failure(e) =>
                System.err.println("failure :"+e.getMessage)
            }
          }

      }
    )
    dom
      .document
      .getElementById("inputFilesDiv")
      .append(inputTag.render)
  }
}
