package com.chatpulse.routes

import cats.effect.*
import cats.syntax.all.*
import com.chatpulse.model.*
import com.chatpulse.service.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.dsl.io.*

object Routes:

  def all(roomService: RoomService, messageService: MessageService): HttpRoutes[IO] =
    healthRoutes <+> roomRoutes(roomService, messageService) <+> messageRoutes(roomService, messageService) <+> statsRoutes(roomService, messageService)

  private val healthRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "api" / "health" =>
      Ok(Map("status" -> "ok", "service" -> "ChatPulse").asJson)
  }

  private def statsRoutes(roomService: RoomService, messageService: MessageService): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root / "api" / "stats" =>
        for {
          rc <- roomService.count
          mc <- messageService.totalCount
          am <- roomService.allMembers
          resp <- Ok(Stats(rc, mc, am.size).asJson)
        } yield resp
    }

  private def roomRoutes(roomService: RoomService, messageService: MessageService): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case req @ POST -> Root / "api" / "rooms" =>
        req.as[CreateRoomRequest].flatMap { body =>
          if (body.name.isBlank)
            BadRequest(ErrorResponse("name is required").asJson)
          else
            roomService.create(body.name).flatMap(room => Created(room.asJson))
        }

      case GET -> Root / "api" / "rooms" =>
        roomService.list.flatMap(rooms => Ok(rooms.asJson))

      case GET -> Root / "api" / "rooms" / id =>
        roomService.get(id).flatMap {
          case Some(room) => Ok(room.asJson)
          case None => NotFound(ErrorResponse("room not found").asJson)
        }

      case DELETE -> Root / "api" / "rooms" / id =>
        roomService.delete(id).flatMap {
          case true => messageService.deleteRoom(id) *> NoContent()
          case false => NotFound(ErrorResponse("room not found").asJson)
        }

      case req @ POST -> Root / "api" / "rooms" / id / "join" =>
        req.as[JoinLeaveRequest].flatMap { body =>
          if (body.userId.isBlank)
            BadRequest(ErrorResponse("userId is required").asJson)
          else
            roomService.join(id, body.userId).flatMap {
              case Right(room) => Ok(room.asJson)
              case Left(err) => NotFound(ErrorResponse(err).asJson)
            }
        }

      case req @ POST -> Root / "api" / "rooms" / id / "leave" =>
        req.as[JoinLeaveRequest].flatMap { body =>
          if (body.userId.isBlank)
            BadRequest(ErrorResponse("userId is required").asJson)
          else
            roomService.leave(id, body.userId).flatMap {
              case Right(room) => Ok(room.asJson)
              case Left(err) => NotFound(ErrorResponse(err).asJson)
            }
        }
    }

  private def messageRoutes(roomService: RoomService, messageService: MessageService): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case req @ POST -> Root / "api" / "rooms" / roomId / "messages" =>
        req.as[SendMessageRequest].flatMap { body =>
          roomService.get(roomId).flatMap {
            case None => NotFound(ErrorResponse("room not found").asJson)
            case Some(_) =>
              if (body.sender.isBlank || body.content.isBlank)
                BadRequest(ErrorResponse("sender and content are required").asJson)
              else
                messageService.send(roomId, body.sender, body.content)
                  .flatMap(msg => Created(msg.asJson))
          }
        }

      case GET -> Root / "api" / "rooms" / roomId / "messages" :? LimitParam(limit) =>
        roomService.get(roomId).flatMap {
          case None => NotFound(ErrorResponse("room not found").asJson)
          case Some(_) =>
            messageService.history(roomId, limit.getOrElse(50))
              .flatMap(msgs => Ok(msgs.asJson))
        }

      case GET -> Root / "api" / "rooms" / roomId / "messages" =>
        roomService.get(roomId).flatMap {
          case None => NotFound(ErrorResponse("room not found").asJson)
          case Some(_) =>
            messageService.history(roomId, 50)
              .flatMap(msgs => Ok(msgs.asJson))
        }
    }

  private object LimitParam extends OptionalQueryParamDecoderMatcher[Int]("limit")
