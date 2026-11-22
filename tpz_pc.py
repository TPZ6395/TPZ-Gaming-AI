import hashlib, json, os, threading, time, webbrowser, urllib.parse
import tkinter as tk
from tkinter import messagebox
import pyautogui, mss, requests

CFG_FILE = "tpz_pc_config.json"
DEFAULT_CFG = {
    "user": "Player", "color": "Royal Blue", "game": "Minecraft",
    "memory": "", "key": "", "owner": False, "vip": False,
    "trial_end": 0, "used_gift": False, "trust": 85, "hurt": False
}

def load_cfg():
    if os.path.exists(CFG_FILE):
        try:
            with open(CFG_FILE, "r") as f:
                return {**DEFAULT_CFG, **json.load(f)}
        except Exception:
            pass
    return dict(DEFAULT_CFG)

def save_cfg(c):
    with open(CFG_FILE, "w") as f:
        json.dump(c, f, indent=2)

def make_vip_code(tag):
    clean = (tag.strip().upper() or "PLAYER")
    h = hashlib.sha256(f"TPZ_SECRET_6395_ZW:{clean}".encode()).hexdigest().upper()
    return f"TPZ-{h[:4]}-{h[4:8]}"
class TpzPCApp:
    def __init__(self, root):
        self.root = root
        self.cfg = load_cfg()
        self.auto_play = False
        self.angle = 0
        root.title("TPZ v3.0 // PC NEURAL CO-PILOT")
        root.geometry("460x720")
        root.configure(bg="#060B19")
        root.attributes("-topmost", True)

        top = tk.Frame(root, bg="#060B19")
        top.pack(pady=10)
        self.canvas = tk.Canvas(top, width=70, height=70, bg="#060B19", highlightthickness=0)
        self.canvas.pack(side="left", padx=8)
        tk.Label(top, text="TPZ PC // CO-PILOT", font=("Consolas", 16, "bold"),
                 fg="#00E5FF", bg="#060B19").pack(side="left")
        self.spin_ring()

        is_vip = self.cfg["owner"] or self.cfg["vip"] or time.time() < self.cfg["trial_end"]
        tier = "OWNER GOD-MODE" if self.cfg["owner"] else ("VIP PRO" if is_vip else "FREE TIER")
        tk.Label(root, text=f"{tier} | BOND: {self.cfg['trust']}%",
                 font=("Consolas", 11, "bold"), fg="#00E676", bg="#060B19").pack(pady=4)

        self.name_in = self.add_box("Gamer Tag", self.cfg["user"])
        self.col_in = self.add_box("Favorite Color", self.cfg["color"])
        self.game_in = self.add_box("Main Game", self.cfg["game"])
        self.mem_in = self.add_box("Memory Notes", self.cfg["memory"])
        self.key_in = self.add_box("Google AI Studio Key (AQ...)", self.cfg["key"])
        self.code_in = self.add_box("VIP Code or Owner Passcode", "")

        tk.Button(root, text="UNLOCK CODE / OWNER ACCESS", bg="#2962FF", fg="white",
                  font=("Consolas", 10, "bold"), command=self.unlock).pack(fill="x", padx=24, pady=5)
        tk.Button(root, text="BUY VIP (EMAIL OWNER FOR CODE)", bg="#00C853", fg="black",
                  font=("Consolas", 10, "bold"), command=self.buy_vip).pack(fill="x", padx=24, pady=5)

        self.auto_btn = tk.Button(root, text="START EYES + HANDS AUTO-PLAY", bg="#00B0FF",
                                  fg="black", font=("Consolas", 10, "bold"), command=self.toggle_play)
        self.auto_btn.pack(fill="x", padx=24, pady=6)

        self.chat_out = tk.Text(root, height=6, bg="#0D162E", fg="#00E5FF", font=("Consolas", 10))
        self.chat_out.pack(fill="x", padx=24, pady=6)
        self.chat_in = self.add_box("Ask TPZ anything...", "")
        tk.Button(root, text="SEND TO TPZ BRAIN", bg="#2962FF", fg="white",
                  font=("Consolas", 10, "bold"), command=self.ask_ai).pack(fill="x", padx=24, pady=4)

        if self.cfg["owner"]:
            self.buyer_in = self.add_box("Buyer's Exact Gamer Tag", "")
            tk.Button(root, text="GENERATE UNGUESSABLE VIP CODE", bg="#00E676", fg="black",
                      font=("Consolas", 10, "bold"), command=self.gen_code).pack(fill="x", padx=24, pady=4)

    def add_box(self, label, val):
        tk.Label(self.root, text=label, fg="#7986CB", bg="#060B19", font=("Consolas", 9)).pack(anchor="w", padx=24)
        e = tk.Entry(self.root, bg="#0D162E", fg="white", insertbackground="white", font=("Consolas", 10))
        e.insert(0, val)
        e.pack(fill="x", padx=24, pady=2)
        return e

    def spin_ring(self):
        self.canvas.delete("all")
        self.canvas.create_arc(8, 8, 62, 62, start=self.angle, extent=270, style="arc", outline="#2962FF", width=4)
        self.canvas.create_text(35, 35, text="TPZ", fill="#00E5FF", font=("Consolas", 11, "bold"))
        self.angle = (self.angle + 12) % 360
        self.root.after(40, self.spin_ring)

    def save_fields(self):
        self.cfg["user"] = self.name_in.get().strip() or "Player"
        self.cfg["color"] = self.col_in.get().strip() or "Royal Blue"
        self.cfg["game"] = self.game_in.get().strip() or "Minecraft"
        self.cfg["memory"] = self.mem_in.get().strip()
        self.cfg["key"] = self.key_in.get().strip()
        save_cfg(self.cfg)

    def unlock(self):
        self.save_fields()
        c = self.code_in.get().strip().upper()
        if c == "TPZ-OWNER-6395":
            self.cfg["owner"] = self.cfg["vip"] = True
            save_cfg(self.cfg)
            messagebox.showinfo("TPZ", "OWNER GOD-MODE UNLOCKED! Restart app to view Vault.")
        elif c == "TPZ-GIFT24" and not self.cfg["used_gift"]:
            self.cfg["used_gift"] = True
            self.cfg["trial_end"] = time.time() + 86400
            save_cfg(self.cfg)
            messagebox.showinfo("TPZ", "24-Hour VIP Pass Activated!")
        elif c == make_vip_code(self.cfg["user"]):
            self.cfg["vip"] = True
            save_cfg(self.cfg)
            messagebox.showinfo("TPZ", f"VIP PRO UNLOCKED FOR {self.cfg['user']}!")
        else:
            messagebox.showwarning("TPZ", "Invalid Code or Gamer Tag!")

    def buy_vip(self):
        tag = urllib.parse.quote(self.name_in.get().strip() or "Player")
        webbrowser.open(f"mailto:tpzish1234567890@://gmail.com{tag}")

    def gen_code(self):
        b = self.buyer_in.get().strip()
        if b:
            messagebox.showinfo("Owner Vault", f"Buyer: {b}\nUnique VIP Code: {make_vip_code(b)}")

    def toggle_play(self):
        self.save_fields()
        self.auto_play = not self.auto_play
        self.auto_btn.config(text="STOP AUTO-PLAY" if self.auto_play else "START EYES + HANDS AUTO-PLAY")
        if self.auto_play:
            threading.Thread(target=self.play_loop, daemon=True).start()

    def play_loop(self):
        is_vip = self.cfg["owner"] or self.cfg["vip"] or time.time() < self.cfg["trial_end"]
        while self.auto_play:
            try:
                if "minecraft" in self.cfg["game"].lower():
                    pyautogui.keyDown("w"); time.sleep(0.4); pyautogui.keyUp("w")
                    if is_vip:
                        pyautogui.click(button="left"); time.sleep(0.15); pyautogui.click(button="left")
                else:
                    if is_vip:
                        pyautogui.click()
                time.sleep(1.5)
            except Exception:
                break

    def ask_ai(self):
        self.save_fields()
        q = self.chat_in.get().strip()
        if not q or not self.cfg["key"]:
            return
        self.chat_in.delete(0, "end")
        threading.Thread(target=self._call_gemini, args=(q,), daemon=True).start()

    def _call_gemini(self, q):
        sys_p = f"You are TPZ, smart futuristic gaming AI for {self.cfg['user']}. Fav color: {self.cfg['color']}. Game: {self.cfg['game']}. Memory: {self.cfg['memory']}."
        url = "https://googleapis.com"
        headers = {"Authorization": f"Bearer {self.cfg['key']}", "Content-Type": "application/json"}
        body = {"model": "gemini-2.5-flash", "messages": [{"role": "system", "content": sys_p}, {"role": "user", "content": q}]}
        try:
            r = requests.post(url, headers=headers, json=body, timeout=20).json()
            ans = r["choices"][0]["message"]["content"]
        except Exception as e:
            ans = f"Error: {e}"
        self.chat_out.insert("end", f"\nTPZ: {ans}\n")
        self.chat_out.see("end")

if __name__ == "__main__":
    root = tk.Tk()
    TpzPCApp(root)
    root.mainloop()
