# JUnit 5 & Mockito Unit Testing Reference Guide

This guide details best practices for writing unit tests for Spring Boot applications using **JUnit 5**, **Mockito**, and **AssertJ**.

---

## 1. Setup & Annotations

To enable Mockito annotations in JUnit 5, annotate your test class with `@ExtendWith(MockitoExtension.class)`.

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    // Test cases go here
}
```

---

## 2. Declaring Mocks & InjectMocks

- **`@Mock`**: Creates a mock instance of a class/interface. Mockito returns default values (null, false, empty lists, etc.) unless instructed otherwise.
- **`@InjectMocks`**: Creates an instance of the class under test and automatically injects all declared `@Mock` fields into it (via constructor, setter, or field injection).
  
> [!IMPORTANT]
> **Mockito Constraint:** `@InjectMocks` **must** be declared on a concrete class type (e.g., `MyServiceImpl`), not an interface (e.g., `MyService`). Mockito cannot instantiate interfaces.

```java
@Mock 
private MyRepository myRepository; // Mock dependency

@InjectMocks 
private MyServiceImpl myService; // Concrete implementation of MyService
```

---

## 3. Mock Stubbing (Behavior Setup)

Use `when(...)` to define what a mock should do when its methods are called.

### A. Simple Return Value
```java
when(myRepository.findById(1L)).thenReturn(Optional.of(new Entity()));
```

### B. Dynamically Mocking Saves/Inserts
When testing creation/save methods, use `thenAnswer` to mock auto-generated IDs or verify fields that get updated dynamically:
```java
when(myRepository.save(any(Entity.class))).thenAnswer(invocation -> {
    Entity entity = invocation.getArgument(0);
    entity.setId(100L); // Mock the database auto-generating an ID
    return entity;
});
```

### C. Throwing Exceptions
```java
when(myRepository.save(any())).thenThrow(new RuntimeException("Database error"));
```

---

## 4. Verification

Use `verify(...)` to confirm that the class under test interacted with the mocks in the expected way.

### A. Simple Verification
Verify a method was called exactly once:
```java
verify(myRepository).findById(1L);
// Equivalent to:
verify(myRepository, times(1)).findById(1L);
```

### B. Verification with Argument Matchers
Verify a method was called with a matching argument structure:
```java
verify(myRepository).save(argThat(entity -> 
    "Expected Name".equals(entity.getName()) && 
    entity.getAge() > 18
));
```

### C. Asserting No Interactions
Verify a mock was never called:
```java
verify(myRepository, never()).delete(any());
```

---

## 5. Assertions (AssertJ)

AssertJ provides highly readable fluent assertions.

```java
import static org.assertj.core.api.Assertions.*;

// Value checking
assertThat(result).isNotNull();
assertThat(result.getId()).isEqualTo(100L);

// Collection checking
assertThat(list).hasSize(3)
                .contains(item1)
                .doesNotContain(item2);

// Exceptions checking
assertThatThrownBy(() -> myService.doSomething(invalidId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("not found");
```
