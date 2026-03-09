package com.chatpulse

import cats.effect.*
import com.chatpulse.service.*
import munit.CatsEffectSuite

class ServiceTest extends CatsEffectSuite:

  test("create room") {
    for
      svc <- RoomService.make
      room <- svc.create("General")
    yield
      assertEquals(room.name, "General")
      assert(room.id.nonEmpty)
      assert(room.members.isEmpty)
  }

  test("list rooms") {
    for
      svc <- RoomService.make
      _ <- svc.create("Room A")
      _ <- svc.create("Room B")
      rooms <- svc.list
    yield assertEquals(rooms.size, 2)
  }

  test("get room") {
    for
      svc <- RoomService.make
      room <- svc.create("Test")
      found <- svc.get(room.id)
    yield assertEquals(found.map(_.name), Some("Test"))
  }

  test("get room not found") {
    for
      svc <- RoomService.make
      found <- svc.get("nonexistent")
    yield assertEquals(found, None)
  }

  test("delete room") {
    for
      svc <- RoomService.make
      room <- svc.create("Delete Me")
      deleted <- svc.delete(room.id)
      found <- svc.get(room.id)
    yield
      assert(deleted)
      assertEquals(found, None)
  }

  test("delete room not found") {
    for
      svc <- RoomService.make
      deleted <- svc.delete("nonexistent")
    yield assert(!deleted)
  }

  test("join room") {
    for
      svc <- RoomService.make
      room <- svc.create("Chat")
      result <- svc.join(room.id, "alice")
    yield
      assert(result.isRight)
      assertEquals(result.toOption.get.members, Set("alice"))
  }

  test("join room not found") {
    for
      svc <- RoomService.make
      result <- svc.join("bad-id", "alice")
    yield assert(result.isLeft)
  }

  test("leave room") {
    for
      svc <- RoomService.make
      room <- svc.create("Chat")
      _ <- svc.join(room.id, "alice")
      result <- svc.leave(room.id, "alice")
    yield
      assert(result.isRight)
      assert(result.toOption.get.members.isEmpty)
  }

  test("leave room not member") {
    for
      svc <- RoomService.make
      room <- svc.create("Chat")
      result <- svc.leave(room.id, "bob")
    yield assert(result.isLeft)
  }

  test("send message") {
    for
      svc <- MessageService.make
      msg <- svc.send("room1", "alice", "Hello!")
    yield
      assertEquals(msg.sender, "alice")
      assertEquals(msg.content, "Hello!")
      assertEquals(msg.roomId, "room1")
  }

  test("message history") {
    for
      svc <- MessageService.make
      _ <- svc.send("room1", "alice", "First")
      _ <- svc.send("room1", "bob", "Second")
      _ <- svc.send("room1", "alice", "Third")
      history <- svc.history("room1", 50)
    yield
      assertEquals(history.size, 3)
      assertEquals(history.head.content, "First")
      assertEquals(history.last.content, "Third")
  }

  test("message history with limit") {
    for
      svc <- MessageService.make
      _ <- svc.send("room1", "alice", "One")
      _ <- svc.send("room1", "bob", "Two")
      _ <- svc.send("room1", "alice", "Three")
      history <- svc.history("room1", 2)
    yield
      assertEquals(history.size, 2)
      assertEquals(history.head.content, "Two")
  }

  test("message count") {
    for
      svc <- MessageService.make
      _ <- svc.send("room1", "alice", "Hello")
      _ <- svc.send("room1", "bob", "Hi")
      count <- svc.count("room1")
    yield assertEquals(count, 2)
  }

  test("total message count") {
    for
      svc <- MessageService.make
      _ <- svc.send("room1", "alice", "Hello")
      _ <- svc.send("room2", "bob", "Hi")
      total <- svc.totalCount
    yield assertEquals(total, 2L)
  }

  test("delete room messages") {
    for
      svc <- MessageService.make
      _ <- svc.send("room1", "alice", "Hello")
      _ <- svc.deleteRoom("room1")
      history <- svc.history("room1", 50)
    yield assert(history.isEmpty)
  }

  test("room count") {
    for
      svc <- RoomService.make
      _ <- svc.create("A")
      _ <- svc.create("B")
      count <- svc.count
    yield assertEquals(count, 2)
  }

  test("all members across rooms") {
    for
      svc <- RoomService.make
      r1 <- svc.create("Room1")
      r2 <- svc.create("Room2")
      _ <- svc.join(r1.id, "alice")
      _ <- svc.join(r1.id, "bob")
      _ <- svc.join(r2.id, "alice")
      _ <- svc.join(r2.id, "charlie")
      members <- svc.allMembers
    yield assertEquals(members, Set("alice", "bob", "charlie"))
  }
