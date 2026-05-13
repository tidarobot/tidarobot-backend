import argparse
import logging
import random
import sys

from selenium import webdriver
from selenium.common import TimeoutException
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import time

EXIT_BOOKED = 0
EXIT_WAITLISTED = 1
EXIT_FAILED = 2

logging.basicConfig(
    handlers=[
        logging.FileHandler("log.log"),
        logging.StreamHandler(sys.stdout),
    ],
    level=logging.INFO,
    format="%(asctime)s | %(levelname)s | %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)


def create_driver(headless=True):
    options = Options()
    if headless:
        options.add_argument("--headless=new")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--disable-gpu")
    return webdriver.Chrome(options=options)


class ParkingBot:
    def __init__(self, driver):
        self.driver = driver
        self.wait = WebDriverWait(driver, 25)

    def login(self, email, password):
        try:
            email_input = self.wait.until(
                EC.visibility_of_element_located((By.ID, "signInName"))
            )
            email_input.send_keys(email)

            button = self.wait.until(EC.visibility_of_element_located((By.ID, "continue")))
            time.sleep(random.randint(1, 5))
            button.click()

            password_input = self.wait.until(
                EC.visibility_of_element_located((By.ID, "password"))
            )
            time.sleep(random.randint(1, 5))
            password_input.send_keys(password)

            sign_in_button = self.wait.until(
                EC.visibility_of_element_located((By.ID, "next"))
            )
            time.sleep(random.randint(1, 5))
            sign_in_button.click()
        except TimeoutException:
            logging.error("Login timed out")
            raise

    def click_start_if_exists(self):
        try:
            button = self.wait.until(
                EC.element_to_be_clickable((
                    By.XPATH,
                    "//button[contains(normalize-space(), 'Start using Tidaro')]",
                ))
            )
            button.click()
        except TimeoutException:
            logging.info("Onboarding button not found — continuing")

    def click_book_spot(self):
        book_btn = self.wait.until(
            EC.element_to_be_clickable((
                By.XPATH,
                "//a[.//div[normalize-space()='Book a spot']]",
            ))
        )
        book_btn.click()
        logging.info("Clicked: Book a spot")

    def _open_spot_area_dropdown(self):
        picker = self.wait.until(
            EC.presence_of_element_located((By.ID, "parking-spot-zones"))
        )
        container = picker.find_element(By.CLASS_NAME, "dropdown__container")
        main_link = container.find_element(By.CLASS_NAME, "dropdown__main")
        main_link.click()
        logging.info("Dropdown opened")

    def _click_dropdown_option(self, parking_area):
        menu_item = self.wait.until(
            EC.element_to_be_clickable((
                By.XPATH,
                f"//a[contains(@class,'dropdown__menu-item') and normalize-space()='{parking_area}']",
            ))
        )
        menu_item.click()
        logging.info("Selected parking area: %s", parking_area)

    def choose_parking_spot_area(self, parking_area):
        self._open_spot_area_dropdown()
        self._click_dropdown_option(parking_area)

    def _find_parking_day(self, day, month):
        xpath = (
            f"//div[contains(@class,'list__item--between')]"
            f"[.//strong[normalize-space()='{day}'] and .//span[normalize-space()='.{month}']]"
        )
        try:
            return self.wait.until(EC.presence_of_element_located((By.XPATH, xpath)))
        except TimeoutException:
            logging.error("Day %s.%s not found in calendar", day, month)
            return None

    def _get_number_of_spots(self, day_element):
        free_spots_elem = day_element.find_element(
            By.XPATH, ".//span[contains(normalize-space(), 'free spots')]"
        )
        return int(free_spots_elem.text.split(":")[1].strip())

    def book_any_place(self, day, month):
        """Returns True if a spot was booked, False if joined the waiting list."""
        day_element = self._find_parking_day(day, month)
        if day_element is None:
            raise RuntimeError(f"Calendar row for {day}.{month} not found")

        number_of_spots = self._get_number_of_spots(day_element)
        if number_of_spots > 0:
            button = day_element.find_element(
                By.XPATH, ".//button[contains(normalize-space(), 'book any')]"
            )
            button.click()
            logging.info("Booked a spot for %s.%s", day, month)
            return True
        else:
            button = day_element.find_element(
                By.XPATH, ".//button[contains(normalize-space(), 'join the waiting list')]"
            )
            button.click()
            logging.info("Joined waiting list for %s.%s", day, month)
            return False

    def run(self, email, password, parking_area, day, month):
        self.driver.get("https://share.parkanizer.com")
        self.login(email, password)
        self.click_start_if_exists()
        self.click_book_spot()
        self.choose_parking_spot_area(parking_area)
        time.sleep(1)
        self.driver.refresh()
        time.sleep(1)
        self.wait.until(EC.presence_of_element_located((By.CLASS_NAME, "list__item--between")))
        time.sleep(1)
        booked = self.book_any_place(day, month)
        time.sleep(1)
        return EXIT_BOOKED if booked else EXIT_WAITLISTED


def main():

    parser = argparse.ArgumentParser(description="Tidaro parking bot")
    parser.add_argument("--email", required=True)
    parser.add_argument("--password", required=True)
    parser.add_argument("--parking-area", required=True)
    parser.add_argument("--day", required=True, help="Zero-padded day, e.g. 09")
    parser.add_argument("--month", required=True, help="Zero-padded month, e.g. 05")
    parser.add_argument("--no-headless", action="store_true", help="Run browser visibly for debugging")
    args = parser.parse_args()

    driver = create_driver(headless=not args.no_headless)
    try:
        bot = ParkingBot(driver)
        exit_code = bot.run(
            args.email,
            args.password,
            args.parking_area,
            args.day,
            args.month,
        )
    except Exception as e:
        logging.exception("Bot failed: %s", e)
        exit_code = EXIT_FAILED
    finally:
        driver.quit()

    sys.exit(exit_code)


if __name__ == "__main__":
    main()
