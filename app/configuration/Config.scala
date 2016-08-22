package configuration

import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._

object Config {

  val config = ConfigFactory.load()

  object Splitwise {
    private val splitwiseConfig = config.getConfig("splitwise")

    val baseUri = splitwiseConfig.getString("baseUri")

    val consumerKey = splitwiseConfig.getString("consumerKey")

    val consumerSecret = splitwiseConfig.getString("consumerSecret")

    object People {
      private val peopleConfig = splitwiseConfig.getConfig("people")

      val all: Set[String] = peopleConfig.entrySet().map(e => e.getKey.split('.')(0)).toSet

      def accessToken(person: String): String = peopleConfig.getString(s"$person.accessToken")

      def accessSecret(person: String): String = peopleConfig.getString(s"$person.accessSecret")

      def userId(person: String): Long = peopleConfig.getLong(s"$person.userId")
    }
  }

}