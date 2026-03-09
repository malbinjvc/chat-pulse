package com.chatpulse

import cats.effect.*
import com.chatpulse.model.*
import com.chatpulse.routes.*
import com.chatpulse.service.*
import io.circe.*
import io.circe.syntax.*
import munit.CatsEffectSuite
import org.http4s.*
import org.http4s.circe.jsonEncoder
import org.http4s.implicits.*
import org.http4s.server.Router

class RoutesTest extends CatsEffectSuite:

  case class TestEnv(app: HttpApp[IO], rs: RoomService, ms: MessageService)

  private def mkEnv: IO[TestEnv] =
    for {
      rs <- RoomService.make
      ms <- MessageService.make
      routes = Middleware.securityHeaders(Routes.all(rs, ms))
      app = Router("/" -> routes).orNotFound
    } yield TestEnv(app, rs, ms)

  private def jsonReq(method: Method, uri: Uri, body: Json): Request[IO] =
    Request[IO](method, uri).withEntity(body)(jsonEncoder[IO])

  test("health endpoint") {
    for {
      env <- mkEnv
      req = Request[IO](Method.GET, uri"/api/health")
      resp <- env.app.run(req)
      body <- resp.as[String]
    } yield {
      assertEquals(resp.status, Status.Ok)
      assert(body.contains("ok"))
    }
  }

  test("security headers present") {
    for {
      env <- mkEnv
      req = Request[IO](Method.GET, uri"/api/health")
      resp <- env.app.run(req)
    } yield {
      assert(resp.headers.get(org.typelevel.ci.CIString("X-Content-Type-Options")).isDefined)
      assert(resp.headers.get(org.typelevel.ci.CIString("X-Frame-Options")).isDefined)
      assert(resp.headers.get(org.typelevel.ci.CIString("Content-Security-Policy")).isDefined)
      assert(resp.headers.get(org.typelevel.ci.CIString("Strict-Transport-Security")).isDefined)
      assert(resp.headers.get(org.typelevel.ci.CIString("Referrer-Policy")).isDefined)
      assert(resp.headers.get(org.typelevel.ci.CIString("Permissions-Policy")).isDefined)
    }
  }

  test("create room") {
    for {
      env <- mkEnv
      req = jsonReq(Method.POST, uri"/api/rooms", CreateRoomRequest("General").asJson)
      resp <- env.app.run(req)
      respBody <- resp.as[String]
    } yield {
      assertEquals(resp.status, Status.Created)
      assert(respBody.contains("General"))
    }
  }

  test("create room missing name") {
    for {
      env <- mkEnv
      req = jsonReq(Method.POST, uri"/api/rooms", Json.obj("name" -> "".asJson))
      resp <- env.app.run(req)
    } yield assertEquals(resp.status, Status.BadRequest)
  }

  test("list rooms") {
    for {
      env <- mkEnv
      _ <- env.rs.create("Room A")
      _ <- env.rs.create("Room B")
      req = Request[IO](Method.GET, uri"/api/rooms")
      resp <- env.app.run(req)
      body <- resp.as[String]
    } yield {
      assertEquals(resp.status, Status.Ok)
      assert(body.contains("Room A"))
      assert(body.contains("Room B"))
    }
  }

  test("get room") {
    for {
      env <- mkEnv
      room <- env.rs.create("Test Room")
      req = Request[IO](Method.GET, Uri.unsafeFromString(s"/api/rooms/${room.id}"))
      resp <- env.app.run(req)
      body <- resp.as[String]
    } yield {
      assertEquals(resp.status, Status.Ok)
      assert(body.contains("Test Room"))
    }
  }

  test("get room not found") {
    for {
      env <- mkEnv
      req = Request[IO](Method.GET, uri"/api/rooms/nonexistent")
      resp <- env.app.run(req)
    } yield assertEquals(resp.status, Status.NotFound)
  }

  test("delete room") {
    for {
      env <- mkEnv
      room <- env.rs.create("Delete Me")
      req = Request[IO](Method.DELETE, Uri.unsafeFromString(s"/api/rooms/${room.id}"))
      resp <- env.app.run(req)
    } yield assertEquals(resp.status, Status.NoContent)
  }

  test("join room") {
    for {
      env <- mkEnv
      room <- env.rs.create("Chat")
      req = jsonReq(Method.POST, Uri.unsafeFromString(s"/api/rooms/${room.id}/join"),
        JoinLeaveRequest("alice").asJson)
      resp <- env.app.run(req)
      respBody <- resp.as[String]
    } yield {
      assertEquals(resp.status, Status.Ok)
      assert(respBody.contains("alice"))
    }
  }

  test("leave room") {
    for {
      env <- mkEnv
      room <- env.rs.create("Chat")
      _ <- env.rs.join(room.id, "alice")
      req = jsonReq(Method.POST, Uri.unsafeFromString(s"/api/rooms/${room.id}/leave"),
        JoinLeaveRequest("alice").asJson)
      resp <- env.app.run(req)
    } yield assertEquals(resp.status, Status.Ok)
  }

  test("send message") {
    for {
      env <- mkEnv
      room <- env.rs.create("Chat")
      req = jsonReq(Method.POST, Uri.unsafeFromString(s"/api/rooms/${room.id}/messages"),
        SendMessageRequest("alice", "Hello everyone!").asJson)
      resp <- env.app.run(req)
      respBody <- resp.as[String]
    } yield {
      assertEquals(resp.status, Status.Created)
      assert(respBody.contains("Hello everyone!"))
      assert(respBody.contains("alice"))
    }
  }

  test("send message to nonexistent room") {
    for {
      env <- mkEnv
      req = jsonReq(Method.POST, uri"/api/rooms/bad-id/messages",
        SendMessageRequest("alice", "Hello").asJson)
      resp <- env.app.run(req)
    } yield assertEquals(resp.status, Status.NotFound)
  }

  test("get message history") {
    for {
      env <- mkEnv
      room <- env.rs.create("Chat")
      _ <- env.ms.send(room.id, "alice", "First")
      _ <- env.ms.send(room.id, "bob", "Second")
      req = Request[IO](Method.GET, Uri.unsafeFromString(s"/api/rooms/${room.id}/messages"))
      resp <- env.app.run(req)
      body <- resp.as[String]
    } yield {
      assertEquals(resp.status, Status.Ok)
      assert(body.contains("First"))
      assert(body.contains("Second"))
    }
  }

  test("stats endpoint") {
    for {
      env <- mkEnv
      room <- env.rs.create("Stats Room")
      _ <- env.rs.join(room.id, "alice")
      _ <- env.ms.send(room.id, "alice", "Hello")
      req = Request[IO](Method.GET, uri"/api/stats")
      resp <- env.app.run(req)
      body <- resp.as[String]
    } yield {
      assertEquals(resp.status, Status.Ok)
      assert(body.contains("roomCount"))
      assert(body.contains("messageCount"))
    }
  }
