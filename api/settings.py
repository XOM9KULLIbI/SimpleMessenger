from pydantic_settings import BaseSettings, SettingsConfigDict

class DbSettings(BaseSettings):
    user: str
    password: str
    database: str
    host: str
    port: int

    @property
    def db_url(self):
        return f"postgresql+asyncpg://{self.user}:{self.password}@{self.host}:{self.port}/{self.database}"

    model_config = SettingsConfigDict(env_file='C:/Users/owen/Documents/GitHub/SimpleMessenger/api/db_settings.env')


db_settings = DbSettings()

class TokenKey(BaseSettings):
    key: str

    model_config = SettingsConfigDict(env_file="C:/Users/owen/Documents/GitHub/SimpleMessenger/api/key.env")

key = TokenKey()
key = key.key
