# Http Request Tool

Tools for run [JB Http Request file](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html)

Supported features

- [x] Basic http call 
- [ ] Configuration env 
- [x] Response handler scripts


## Build

```shell script
export LEIN_JVM_OPTS='-Dpolyglot.js.ecmascript-version=2020'
lein run
```


For build
> Required installed graal-vm native-image   
```shell script
make build
```

Clean after install 
```shell script
make clean
```

## Run

```shell script
./jb-http-test
```


