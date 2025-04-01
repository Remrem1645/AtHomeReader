const username = localStorage.getItem("username");
if (!username) {
  window.location.href = "/index.html";
}

document.getElementById("user-name")!.textContent = username;

document.getElementById("logout-btn")!.addEventListener("click", async () => {
  await fetch("http://localhost:8080/api/account/logout", {
    method: "POST",
    credentials: "include"
  });
  localStorage.removeItem("username");
  window.location.href = "/index.html";
});

async function fetchBooks() {
  const res = await fetch("http://localhost:8080/api/books/all", {
    credentials: "include",
  });

  if (!res.ok) {
    alert("Failed to load books");
    return;
  }

  const books = await res.json();
  const grid = document.getElementById("book-grid")!;

  for (const book of books) {
    const card = document.createElement("div");
    card.className = "book-card";
    card.innerHTML = `
      <img src="${book.coverImagePath}" alt="${book.title}" />
      <div class="info">
        <h3>${book.title}</h3>
        <p>${book.author || ""}</p>
        <p>Page ${book.currentPage ?? 0}</p>
      </div>
    `;
    card.addEventListener("click", () => {
      window.location.href = `/reader.html?uuid=${book.uuid}`;
    });

    grid.appendChild(card);
  }
}

fetchBooks();
