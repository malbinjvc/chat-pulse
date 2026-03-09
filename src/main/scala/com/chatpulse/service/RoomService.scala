package com.chatpulse.service

import cats.effect.*
import cats.effect.std.AtomicCell
import com.chatpulse.model.Room

class RoomService(rooms: Ref[IO, Map[String, Room]]):

  def create(name: String): IO[Room] =
    val room = Room.create(name)
    rooms.update(_ + (room.id -> room)).as(room)

  def get(id: String): IO[Option[Room]] =
    rooms.get.map(_.get(id))

  def list: IO[List[Room]] =
    rooms.get.map(_.values.toList.sortBy(_.createdAt))

  def delete(id: String): IO[Boolean] =
    rooms.modify { m =>
      if m.contains(id) then (m - id, true)
      else (m, false)
    }

  def join(roomId: String, userId: String): IO[Either[String, Room]] =
    rooms.modify { m =>
      m.get(roomId) match
        case None => (m, Left("room not found"))
        case Some(room) =>
          val updated = room.copy(members = room.members + userId)
          (m + (roomId -> updated), Right(updated))
    }

  def leave(roomId: String, userId: String): IO[Either[String, Room]] =
    rooms.modify { m =>
      m.get(roomId) match
        case None => (m, Left("room not found"))
        case Some(room) =>
          if !room.members.contains(userId) then (m, Left("user not in room"))
          else
            val updated = room.copy(members = room.members - userId)
            (m + (roomId -> updated), Right(updated))
    }

  def count: IO[Int] = rooms.get.map(_.size)

  def allMembers: IO[Set[String]] =
    rooms.get.map(_.values.flatMap(_.members).toSet)

object RoomService:
  def make: IO[RoomService] =
    Ref.of[IO, Map[String, Room]](Map.empty).map(new RoomService(_))
