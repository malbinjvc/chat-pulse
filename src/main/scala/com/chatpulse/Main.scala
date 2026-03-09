package com.chatpulse

import cats.effect.*
import com.chatpulse.routes.*
import com.chatpulse.service.*
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router

object Main extends IOApp:

  override def run(args: List[String]): IO[ExitCode] =
    val portStr = sys.env.getOrElse("PORT", "8080")
    val port = Port.fromString(portStr).getOrElse(port"8080")

    for
      roomService <- RoomService.make
      messageService <- MessageService.make
      routes = Middleware.securityHeaders(Routes.all(roomService, messageService))
      httpApp = Router("/" -> routes).orNotFound
      _ <- EmberServerBuilder
        .default[IO]
        .withHost(host"0.0.0.0")
        .withPort(port)
        .withHttpApp(httpApp)
        .build
        .useForever
    yield ExitCode.Success
