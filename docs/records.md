# Records

Records are a core component of Bendu and are used to represent structured data. Bendu has elementary data types, such as Bool, Char, Float, Int and String, and collection data types such as Tuple and Record.

## Syntax

A record is a collection of fields, each with a name and a value. Records can be defined and used in several ways:

### Creating Records

The simplest record is an empty record:

```bendu-repl
> { }
{}: {}
```

Records with fields are defined by specifying field names and values:

```bendu-repl
> {name: "John", age: 30}
{name: "John", age: 30}: {name: String, age: Int}

> let person = {name: "Alice", age: 25, active: True}
> person
{name: "Alice", age: 25, active: True}: {name: String, age: Int, active: Bool}
```

### Accessing Fields

Fields can be accessed using dot notation:

```bendu-repl
> person.name
"Alice": String

> person.age
25: Int

> {name: "Bob", age: 40}.name
"Bob": String
```

### Updating Records

Records are immutable, but you can create new records based on existing ones using the update syntax:

```bendu-repl
> let updatedPerson = {person | age: 26}
> updatedPerson
{name: "Alice", age: 26, active: True}: {name: String, age: Int, active: Bool}

> {person | name: "Alice Cooper", active: False}
{name: "Alice Cooper", age: 25, active: False}: {name: String, age: Int, active: Bool}
```

### Record Spreading

You can also use the spread operator (`...`) to include all fields from another record:

```bendu-repl
> let person = {name: "Alice", age: 25, position: "Developer"}
> {...person, name: "Alec"}
{name: "Alec", age: 25, position: "Developer"}: {name: String, age: Int, position: String}
```

The computation happens left to right, so field values are overridden by later fields with the same name:

```bendu-repl
> {name: "Alec", ...person}
{name: "Alice", age: 25, position: "Developer"}: {name: String, age: Int, position: String}
```

This allows for flexible record composition and updates:

## Record Operations

### Pattern Matching with Records

Records can be destructured in pattern matching:

```bendu-repl
> let showPerson(p) =
.   "Name: " + p.name + ", Age: " + toString(p.age)

> showPerson(person)
"Name: Alice, Age: 25": String

> let getStatus({active}) =
.   if active -> "Active" | "Inactive"

> getStatus(person)
"Active": String
```

You can also use `match` statements with record patterns, including the ability to extract specific fields while using the spread operator for other fields:

```bendu-repl
> let nameOf(r): String =
.    match r with
.    | {name: n, ...} -> n
fn: [a] ({name: String} <: a) -> String

> nameOf({name: "Alice", age: 25, active: True})
"Alice": String

> nameOf({name: "Bob", department: "Engineering"})
"Bob": String
```

### Using Records with Functions

Records work well as function parameters and return values:

```bendu-repl
> let makePerson(name, age) =
.   {name: name, age: age, active: True}

> makePerson("Charlie", 35)
{name: "Charlie", age: 35, active: True}: {name: String, age: Int, active: Bool}

> let incrementAge(person) =
.   {person | age: person.age + 1}

> incrementAge(person)
{name: "Alice", age: 26, active: True}: {name: String, age: Int, active: Bool}
```

### Records and Higher-Order Functions

Records can be used effectively with higher-order functions:

```bendu-repl
> let users = [
.   {name: "Alice", age: 25, active: True},
.   {name: "Bob", age: 30, active: False},
.   {name: "Charlie", age: 35, active: True}
. ]

> let activeUsers = filter(fn(u) = u.active, users)
> activeUsers
[{name: "Alice", age: 25, active: True}, {name: "Charlie", age: 35, active: True}]: [{name: String, age: Int, active: Bool}]

> let names = map(fn(u) = u.name, users)
> names
["Alice", "Bob", "Charlie"]: [String]
```

## Nested Records

Records can contain other records, allowing for complex data structures:

```bendu-repl
> let employee = {
.   person: {name: "Alice", age: 25},
.   position: "Developer",
.   department: {id: 101, name: "Engineering"}
. }

> employee.person.name
"Alice": String

> employee.department.name
"Engineering": String
```

## Record Type Annotations

You can explicitly annotate record types:

```bendu-repl
> let Person = {name: String, age: Int, active: Bool}
> let hire: Person -> {Person, position: String} = fn(p) = {p, position: "New Hire"}

> hire(person)
{name: "Alice", age: 25, active: True, position: "New Hire"}: {name: String, age: Int, active: Bool, position: String}
```

## Extensible Records

Bendu supports extensible record types, allowing functions to work with any record that has at least the required fields:

```bendu-repl
> let getName: {name: String, a} -> String = fn(r) = r.name

> getName({name: "Alice", age: 25})
"Alice": String

> getName({name: "Bob", id: 123, department: "HR"})
"Bob": String
```

## Comparing Records

Records can be compared for equality:

```bendu-repl
> let p1 = {name: "Alice", age: 25}
> let p2 = {name: "Alice", age: 25}
> let p3 = {name: "Bob", age: 30}

> p1 == p2
True: Bool

> p1 == p3
False: Bool
```

Records in Bendu are designed to be simple yet powerful, providing a clean syntax for creating and manipulating structured data, similar to Elm's approach. They enable type safety while maintaining flexibility and readability in your code.



