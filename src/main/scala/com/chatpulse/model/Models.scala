package com.chatpulse.model

import java.time.Instant
import java.util.UUID
import io.circe.*
import io.circe.generic.semiauto.*

case class Room(
  id: String,
  name: String,
  createdAt: Instant,
  members: Set[String]
)

object Room:
  def create(name: String): Room =
    Room(UUID.randomUUID().toString, name, Instant.now(), Set.empty)
  given Encoder[Room] = deriveEncoder
  given Decoder[Room] = deriveDecoder

case class Message(
  id: String,
  roomId: String,
  sender: String,
  content: String,
  timestamp: Instant
)

object Message:
  def create(roomId: String, sender: String, content: String): Message =
    Message(UUID.randomUUID().toString, roomId, sender, content, Instant.now())
  given Encoder[Message] = deriveEncoder
  given Decoder[Message] = deriveDecoder

case class CreateRoomRequest(name: String)
object CreateRoomRequest:
  given Encoder[CreateRoomRequest] = deriveEncoder
  given Decoder[CreateRoomRequest] = deriveDecoder

case class SendMessageRequest(sender: String, content: String)
object SendMessageRequest:
  given Encoder[SendMessageRequest] = deriveEncoder
  given Decoder[SendMessageRequest] = deriveDecoder

case class JoinLeaveRequest(userId: String)
object JoinLeaveRequest:
  given Encoder[JoinLeaveRequest] = deriveEncoder
  given Decoder[JoinLeaveRequest] = deriveDecoder

case class Stats(
  roomCount: Int,
  messageCount: Long,
  activeMembers: Int
)

object Stats:
  given Encoder[Stats] = deriveEncoder

case class ErrorResponse(error: String)
object ErrorResponse:
  given Encoder[ErrorResponse] = deriveEncoder
