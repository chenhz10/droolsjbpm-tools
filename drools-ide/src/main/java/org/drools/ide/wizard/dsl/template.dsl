#place your comments here - this is just a description for your own purposes.
There exists a Person with name of {name}=Person(name=="{name}")
[when]Person is at least {age} years old and lives in {location}=Person(age > {age}, location=="{location}")
[then]Log {message}=System.out.println("{message}");
