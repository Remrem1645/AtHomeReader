const username = localStorage.getItem("username");
if (!username) {
  window.location.href = "/index.html";
}

const root = document.getElementById("login-root") || document.getElementById("library-root");
if (localStorage.getItem("theme") === "dark") {
  root?.classList.add("dark");
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

const darkToggle = document.getElementById("dark-toggle-btn") as HTMLInputElement;

restoreTheme();
function restoreTheme() {
    const saved = localStorage.getItem("theme");
    console.log(saved);
    if (saved === "dark") {
        root?.classList.add("dark");
        darkToggle.textContent = "🌙";
    }


    darkToggle.addEventListener("click", () => {
        const isDark = root?.classList.toggle("dark");
        darkToggle.textContent = isDark ?  "🌙" : "☀️";
        localStorage.setItem("theme", isDark ? "dark" : "light");
    });
}


const contextMenu = document.createElement("div");
contextMenu.className = "context-menu";
document.body.appendChild(contextMenu);
let currentBookId: string | null = null;

document.addEventListener("click", () => {
  contextMenu.style.display = "none";
});

function showContextMenu(x: number, y: number, uuid: string) {
  contextMenu.innerHTML = `
    <button id="delete-book">🗑️ Delete</button>
  `;
  contextMenu.style.left = `${x}px`;
  contextMenu.style.top = `${y}px`;
  contextMenu.style.display = "flex";
  currentBookId = uuid;

  document.getElementById("edit-book")?.addEventListener("click", () => {
    const newTitle = prompt("New Title:");
    const newAuthor = prompt("New Author:");
    const newDescription = prompt("New Description:");

    if (newTitle || newAuthor || newDescription) {
      fetch(`http://localhost:8080/api/books/${uuid}/edit`, {
        method: "PUT",
        credentials: "include",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          title: newTitle,
          author: newAuthor,
          description: newDescription
        })
      }).then(res => {
        if (res.ok) {
          alert("Book updated.");
          fetchBooks();
        }
      });
    }
    contextMenu.style.display = "none";
  });

  document.getElementById("delete-book")?.addEventListener("click", () => {
    if (confirm("Are you sure you want to delete this book?")) {
      fetch(`http://localhost:8080/api/books/${uuid}/delete`, {
        method: "DELETE",
        credentials: "include"
      }).then(res => {
        if (res.ok) {
          fetchBooks();
        }
      });
    }
    contextMenu.style.display = "none";
  });
}

async function fetchBooks() {
    const grid = document.getElementById("book-grid")!;
    grid.innerHTML = "";

  const res = await fetch("http://localhost:8080/api/books/all", {
    credentials: "include",
  });

  if (!res.ok) {
    alert("Failed to load books");
    return;
  }

  const text = await res.text();
  if (!text) {
    createUploadCard(grid);
    return;
  }
  
  let books;
  try {
    books = JSON.parse(text);
  } catch (e) {
    console.error("Invalid JSON:", text);
    createUploadCard(grid);
    return;
  }

  for (const book of books) {
    const card = document.createElement("div");
    card.className = "book-card";
    card.innerHTML = `
      <img src="${book.coverImageUrl}" alt="${book.title}" />
      <div class="info">
        <h3>${book.title}</h3>
        <p>${book.author || ""}</p>
        <p>Page ${book.currentPage ?? 0}</p>
      </div>
    `;
    card.addEventListener("click", () => {
      window.location.href = `/reader.html?uuid=${book.uuid}`;
    });
    card.addEventListener("contextmenu", (e) => {
      e.preventDefault();
      showContextMenu(e.pageX, e.pageY, book.uuid);
    });

    grid.appendChild(card);
  }
  createUploadCard(grid);
}

function createUploadCard(grid: HTMLElement) {
  const card = document.createElement("div");
  card.className = "book-card upload-card";
  card.innerHTML = `
    <div class="upload-placeholder">
      <span>＋</span>
      <p>Upload Book</p>
      <input type="file" accept=".epub" id="upload-input" hidden />
    </div>
  `;

  card.addEventListener("click", () => {
    const input = card.querySelector("input") as HTMLInputElement;
    input?.click();
  });

  card.querySelector("input")?.addEventListener("change", async (e) => {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (!file) return;

    const formData = new FormData();
    formData.append("file", file);

    const res = await fetch("http://localhost:8080/api/books/upload", {
      method: "POST",
      body: formData,
      credentials: "include",
    });

    if (res.ok) {
      alert("Book uploaded successfully!");
      fetchBooks();
    } else {
      alert("Failed to upload book.");
    }
  });

  grid.appendChild(card);
}

fetchBooks();
