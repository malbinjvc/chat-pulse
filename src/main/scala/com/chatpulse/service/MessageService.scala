package com.chatpulse.service

import cats.effect.*
import com.chatpulse.model.Message

class MessageService(messages: Ref[IO, Map[String, List[Message]]]):

  def send(roomId: String, sender: String, content: String): IO[Message] =
    val msg = Message.create(roomId, sender, content)
    messages.update { m =>
      val existing = m.getOrElse(roomId, List.empty)
      m + (roomId -> (existing :+ msg))
    }.as(msg)

  def history(roomId: String, limit: Int): IO[List[Message]] =
    messages.get.map { m =>
      m.getOrElse(roomId, List.empty).takeRight(limit)
    }

  def count(roomId: String): IO[Int] =
    messages.get.map(_.getOrElse(roomId, List.empty).size)

  def totalCount: IO[Long] =
    messages.get.map(_.values.map(_.size.toLong).sum)

  def deleteRoom(roomId: String): IO[Unit] =
    messages.update(_ - roomId)

object MessageService:
  def make: IO[MessageService] =
    Ref.of[IO, Map[String, List[Message]]](Map.empty).map(new MessageService(_))
