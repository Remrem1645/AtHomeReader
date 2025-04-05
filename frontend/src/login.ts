const usernameInput = document.getElementById("username") as HTMLInputElement;
const passwordInput = document.getElementById("password") as HTMLInputElement;
const loginBtn = document.getElementById("login-btn")!;
const errorEl = document.getElementById("login-error")!;

const root = document.getElementById("login-root") || document.getElementById("library-root");
if (localStorage.getItem("theme") === "dark") {
  root?.classList.add("dark");
}

loginBtn.addEventListener("click", async () => {
  const username = usernameInput.value.trim();
  const password = passwordInput.value.trim();

  if (!username || !password) {
    errorEl.textContent = "Username and password required.";
    return;
  }

  const res = await fetch("http://localhost:8080/api/account/login", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });

  if (res.ok) {
    localStorage.setItem("username", username);
    window.location.href = "/library.html";
  } else {
    errorEl.textContent = "Login failed. Please try again.";
  }
});

[usernameInput, passwordInput].forEach((input) => {
    input.addEventListener("keydown", (e) => {
      if (e.key === "Enter") {
        loginBtn.click();
      }
    });
  });
  