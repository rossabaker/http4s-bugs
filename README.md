# To reproduce

```sh
docker run -p 80:80 kennethreitz/httpbin
sbt run
```

Could cut the Docker dependency by spinning up our own server, but the point is to test the client, so, meh?
