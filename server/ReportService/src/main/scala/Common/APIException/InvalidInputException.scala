package Common.APIException

case class InvalidInputException(message: String) extends Exception(message)
