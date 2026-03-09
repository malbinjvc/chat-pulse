package com.chatpulse.routes

import cats.data.Kleisli
import cats.effect.*
import org.http4s.*
import org.typelevel.ci.*

object Middleware:

  def securityHeaders(routes: HttpRoutes[IO]): HttpRoutes[IO] =
    Kleisli { req =>
      routes.run(req).map { response =>
        response.putHeaders(
          Header.Raw(ci"X-Content-Type-Options", "nosniff"),
          Header.Raw(ci"X-Frame-Options", "DENY"),
          Header.Raw(ci"X-XSS-Protection", "0"),
          Header.Raw(ci"Content-Security-Policy", "default-src 'none'"),
          Header.Raw(ci"Strict-Transport-Security", "max-age=31536000; includeSubDomains"),
          Header.Raw(ci"Referrer-Policy", "strict-origin-when-cross-origin"),
          Header.Raw(ci"Permissions-Policy", "camera=(), microphone=(), geolocation=()")
        )
      }
    }
