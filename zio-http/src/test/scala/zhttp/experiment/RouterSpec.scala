package zhttp.experiment

import zhttp.experiment.Router._
import zhttp.http.Method
import zio.test.Assertion._
import zio.test._

object RouterSpec extends DefaultRunnableSpec {
  def spec = suite("Router")(
    test("Method")(assert(OnlyMethod(Method.GET))(isSubtype[Router[Unit]])),
//    test("String")(assert(OnlyString("a"))(isSubtype[Router[Unit]])),
//    test("Method and string")(assert(Method.GET / "string")(isSubtype[Router[Unit]])),
//    test("Method, String and Int")(assert(Method.GET / "string" / Arg[Int])(isSubtype[Router[Int]])),
//    test("Method, Int and String")(assert(Method.GET / Arg[Int] / "string")(isSubtype[Router[Int]])),
//    test("Method, Int,String and Int")(
//      assert(Method.GET / Arg[Int] / "string" / Arg[Int])(isSubtype[Router[(Int, Int)]]),
//    ),
  )
}
