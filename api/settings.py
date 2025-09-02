from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

BASE_DIR = Path(__file__).resolve().parent

class DbSettings(BaseSettings):
    user: str
    password: str
    database: str
    host: str
    port: int

    @property
    def db_url(self):
        return f"postgresql+asyncpg://{self.user}:{self.password}@{self.host}:{self.port}/{self.database}"

    model_config = SettingsConfigDict(env_file=BASE_DIR / 'db_set_new.env')


db_settings = DbSettings()

class TokenKey(BaseSettings):
    key: str

    model_config = SettingsConfigDict(env_file=BASE_DIR / "key.env")

key = TokenKey()
key = key.key
