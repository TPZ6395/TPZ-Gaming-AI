import hashlib, json, math, os, random, threading, time, webbrowser, urllib.parse
import tkinter as tk
from tkinter import messagebox
import pyautogui, requests

CFG_FILE = "tpz_pc_config.json"
DEFAULT_CFG = {
    "setup_done": False, "user": "Player", "key": "",
    "notes": "", "game": "Minecraft",
    "ai_rules": "Kind, ultra-smart futuristic gaming co-pilot. Pronounce TPZ as Tea Pee Zet.",
    "owner": False, "vip": False, "trial_end": 0, "used_gift": False, "trust": 85
}
QUIET_QUESTIONS = [
    "Hey, while things are quiet—what is your favorite color so I can customize your builds?",
    "Random question while we chill: what sport do you love watching or playing?",
    "Quick one—what is your favorite TV show or anime right now?",
    "Just curious so I know you better: what kind of music gets you hyped for gaming?",
    "While we have a quiet moment—what is your dream car or dream setup?"
]

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
        self.angle = 0
        self.thinking = False
        self.auto_play = False
        self.last_chat = time.time()
        self.waiting_personal = False
        root.title("TPZ v4.1 // TEA PEE ZET")
        root.geometry("480x780")
        root.configure(bg="#041230")
        root.attributes("-topmost", True)
        self.show_splash()

    def clear_ui(self):
        for w in self.root.winfo_children():
            w.destroy()

    def draw_spinning_letters(self, canvas, cx, cy, r=28):
        canvas.delete("spin")
        for idx, ch in enumerate(["T", "P", "Z"]):
            a = math.radians(self.angle + idx * 120)
            x = cx + r * math.cos(a)
            y = cy + r * math.sin(a)
            canvas.create_text(x, y, text=ch, fill="#00E5FF",
                               font=("Consolas", 18, "bold"), tags="spin")
        self.angle = (self.angle + 10) % 360

    def show_splash(self):
        self.clear_ui()
        c = tk.Canvas(self.root, width=300, height=300,
                      bg="#041230", highlightthickness=0)
        c.pack(expand=True)
        end_t = time.time() + 10.0
        def tick():
            if time.time() < end_t:
                self.draw_spinning_letters(c, 150, 150, 42)
                self.root.after(40, tick)
            elif not self.cfg["setup_done"]:
                self.show_first_setup()
            else:
                self.show_main_hub(greet=True)
        tick()

    def show_first_setup(self):
        self.clear_ui()
        tk.Label(self.root, text="WELCOME TO TPZ",
                 font=("Consolas", 18, "bold"),
                 fg="#00E5FF", bg="#041230").pack(pady=30)
        name_e = self.make_entry("Name for Tea Pee Zet to call you", "")
        key_e = self.make_entry("Google AI Studio API Key", "")
        def finish():
            self.cfg["user"] = name_e.get().strip() or "Player"
            self.cfg["key"] = key_e.get().strip()
            self.cfg["setup_done"] = True
            save_cfg(self.cfg)
            self.show_main_hub(greet=True)
        tk.Button(self.root, text="ENTER TPZ ARENA", bg="#2962FF",
                  fg="white", font=("Consolas", 12, "bold"),
                  command=finish).pack(fill="x", padx=40, pady=24)

    def show_main_hub(self, greet=False):
        self.clear_ui()
        self.last_chat = time.time()
        top = tk.Frame(self.root, bg="#041230")
        top.pack(fill="x", padx=16, pady=10)
        tk.Label(top, text=f"PLAYER: {self.cfg['user'].upper()}",
                 fg="#00E5FF", bg="#041230",
                 font=("Consolas", 11, "bold")).pack(side="left")
        tk.Button(top, text="SETTINGS", bg="#2962FF", fg="white",
                  font=("Consolas", 10, "bold"),
                  command=self.open_settings).pack(side="right")

        self.bubble_c = tk.Canvas(self.root, width=170, height=170,
                                  bg="#041230", highlightthickness=0)
        self.bubble_c.pack(pady=8)
        self.animate_bubble()

        self.chat_out = tk.Text(self.root, height=8, bg="#091D4A",
                                fg="#E0F7FA", font=("Consolas", 10))
        self.chat_out.pack(fill="x", padx=20, pady=6)
        self.chat_in = self.make_entry("Talk to Tea Pee Zet...", "")
        self.chat_in.bind("<Return>", lambda e: self.ask_ai())
        tk.Button(self.root, text="SEND TO TPZ", bg="#00B0FF", fg="black",
                  font=("Consolas", 10, "bold"),
                  command=self.ask_ai).pack(fill="x", padx=20, pady=4)

        if self.cfg["owner"]:
            god = tk.LabelFrame(self.root, text="OWNER GOD-MODE DASHBOARD",
                                fg="#00E676", bg="#041230",
                                font=("Consolas", 10, "bold"))
            god.pack(fill="x", padx=20, pady=8)
            self.rules_e = self.make_entry(
                "AI Behavior & Updates", self.cfg["ai_rules"], parent=god)
            self.buyer_e = self.make_entry(
                "Buyer Gamer Tag for VIP Code", "", parent=god)
            tk.Button(god, text="APPLY UPDATE & GENERATE CODE",
                      bg="#00E676", fg="black", font=("Consolas", 9, "bold"),
                      command=self.apply_god_mode).pack(fill="x", padx=12, pady=6)

        if greet:
            msg = f"Hello {self.cfg['user']}!"
            self.chat_out.insert("end", f"TPZ: {msg}\n")
            self.speak(msg)
        self.quiet_watcher()
    def quiet_watcher(self):
        if not hasattr(self, "chat_out") or not self.chat_out.winfo_exists():
            return
        if not self.thinking and (time.time() - self.last_chat) > 45:
            self.last_chat = time.time()
            self.waiting_personal = True
            q = random.choice(QUIET_QUESTIONS)
            self.chat_out.insert("end", f"\nTPZ: {q}\n")
            self.chat_out.see("end")
            self.speak(q)
        self.root.after(5000, self.quiet_watcher)

    def animate_bubble(self):
        if not hasattr(self, "bubble_c") or not self.bubble_c.winfo_exists():
            return
        self.bubble_c.delete("all")
        self.bubble_c.create_oval(15, 15, 155, 155,
                                  fill="#0D2B6B", outline="#00E5FF", width=4)
        if self.thinking:
            self.draw_spinning_letters(self.bubble_c, 85, 85, 30)
        else:
            self.bubble_c.create_text(85, 85, text="TPZ", fill="#00E5FF",
                                      font=("Consolas", 22, "bold"))
        self.root.after(40, self.animate_bubble)

    def open_settings(self):
        win = tk.Toplevel(self.root)
        win.title("TPZ Settings")
        win.geometry("380x440")
        win.configure(bg="#041230")
        win.attributes("-topmost", True)
        code_e = self.make_entry("Owner Passcode or VIP Code", "", parent=win)
        def check():
            c = code_e.get().strip().upper()
            win.destroy()
            self.brief_load(lambda: self.process_code(c))
        tk.Button(win, text="SUBMIT CODE", bg="#2962FF", fg="white",
                  font=("Consolas", 10, "bold"),
                  command=check).pack(fill="x", padx=20, pady=6)
        mail = f"mailto:yourname@://gmail.com{urllib.parse.quote(self.cfg['user'])}"
        tk.Button(win, text="BUY VIP (EMAIL OWNER)", bg="#00C853", fg="black",
                  font=("Consolas", 10, "bold"),
                  command=lambda: webbrowser.open(mail)).pack(fill="x", padx=20, pady=6)
        feats = "Quiet-Moment Questions, Smart Memory, Auto-Play & God-Mode"
        tk.Button(win, text="SEE TPZ FEATURES", bg="#00B0FF", fg="black",
                  font=("Consolas", 10, "bold"),
                  command=lambda: messagebox.showinfo("TPZ Features", feats)).pack(fill="x", padx=20, pady=6)
        tk.Button(win, text="TOGGLE AUTO-PLAY FOR ME", bg="#7C4DFF", fg="white",
                  font=("Consolas", 10, "bold"),
                  command=self.toggle_play).pack(fill="x", padx=20, pady=6)

    def brief_load(self, done_cb):
        self.clear_ui()
        c = tk.Canvas(self.root, width=220, height=220,
                      bg="#041230", highlightthickness=0)
        c.pack(expand=True)
        end_t = time.time() + 1.5
        def tick():
            if time.time() < end_t:
                self.draw_spinning_letters(c, 110, 110, 32)
                self.root.after(40, tick)
            else:
                done_cb()
        tick()

    def process_code(self, c):
        if c == "TPZ-OWNER-6395":
            self.cfg["owner"] = self.cfg["vip"] = True
            save_cfg(self.cfg)
            messagebox.showinfo("God-Mode", "OWNER GOD-MODE ACTIVATED!")
        elif c == "TPZ-GIFT24" and not self.cfg["used_gift"]:
            self.cfg["used_gift"] = True
            self.cfg["trial_end"] = time.time() + 86400
            save_cfg(self.cfg)
            messagebox.showinfo("VIP", "24-Hour VIP Gift Activated!")
        elif c == make_vip_code(self.cfg["user"]):
            self.cfg["vip"] = True
            save_cfg(self.cfg)
            messagebox.showinfo("VIP", "VIP Pro Unlocked!")
        else:
            messagebox.showwarning("TPZ", "Invalid Code!")
        self.show_main_hub(greet=False)

    def apply_god_mode(self):
        self.cfg["ai_rules"] = self.rules_e.get().strip()
        save_cfg(self.cfg)
        b = self.buyer_e.get().strip()
        msg = "AI rules updated live!"
        if b:
            msg += f"\nBuyer {b} VIP Code: {make_vip_code(b)}"
        messagebox.showinfo("Owner God-Mode", msg)

    def toggle_play(self):
        self.auto_play = not self.auto_play
        if self.auto_play:
            threading.Thread(target=self.play_loop, daemon=True).start()

    def play_loop(self):
        is_vip = self.cfg["owner"] or self.cfg["vip"] or time.time() < self.cfg["trial_end"]
        while self.auto_play:
            pyautogui.keyDown("w"); time.sleep(0.4); pyautogui.keyUp("w")
            if is_vip:
                pyautogui.click()
            time.sleep(1.5)

    def make_entry(self, label, val, parent=None):
        p = parent or self.root
        tk.Label(p, text=label, fg="#82B1FF", bg="#041230",
                 font=("Consolas", 9)).pack(anchor="w", padx=20)
        e = tk.Entry(p, bg="#091D4A", fg="white",
                     insertbackground="white", font=("Consolas", 10))
        e.insert(0, val)
        e.pack(fill="x", padx=20, pady=3)
        return e

    def speak(self, txt):
        def _run():
            try:
                import pyttsx3
                eng = pyttsx3.init()
                eng.say(txt.replace("TPZ", "Tea Pee Zet").replace("tpz", "Tea Pee Zet"))
                eng.runAndWait()
            except Exception:
                pass
        threading.Thread(target=_run, daemon=True).start()

    def ask_ai(self):
        q = self.chat_in.get().strip()
        if not q:
            return
        self.last_chat = time.time()
        self.chat_in.delete(0, "end")
        self.chat_out.insert("end", f"\n{self.cfg['user']}: {q}\n")
        self.chat_out.see("end")
        if self.waiting_personal:
            self.waiting_personal = False
            self.cfg["notes"] = (self.cfg["notes"] + " | " + q)[-300:]
            save_cfg(self.cfg)
        self.thinking = True
        threading.Thread(target=self._call_gemini, args=(q,), daemon=True).start()

    def _call_gemini(self, q):
        sys_p = (f"You are TPZ (Tea Pee Zet) talking to {self.cfg['user']}. "
                 f"Rules: {self.cfg['ai_rules']}. Personal facts learned: {self.cfg['notes']}. "
                 f"Use these personal facts naturally when helping them!")
        url = f"https://googleapis.com{self.cfg['key']}"
        try:
            r = requests.post(url, json={"contents": [{"parts": [{"text": f"{sys_p}\n\nUser: {q}"}]}]}, timeout=20).json()
            ans = r["error"]["message"] if "error" in r else r["candidates"]["content"]["parts"]["text"]
        except Exception as e:
            ans = f"Error: {e}"
        self.thinking = False
        self.chat_out.insert("end", f"TPZ: {ans}\n")
        self.chat_out.see("end")
        self.speak(ans[:160])

if __name__ == "__main__":
    root = tk.Tk()
    TpzPCApp(root)
    root.mainloop()




