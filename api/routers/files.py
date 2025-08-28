import io
import os
from typing import Annotated

from fastapi import APIRouter, UploadFile, File, Depends, HTTPException
from pydantic import PositiveInt
from starlette import status
from starlette.responses import StreamingResponse

from api.db.queries import ORM
from api.dependecies.auth import get_current_active_user
from api.schemas.file_schema import FileMeta
from api.schemas.user_schemas import User

files_router = APIRouter(prefix="/files", tags=["files"])

ALLOWED_EXTENSIONS = {'.jpg', '.jpeg', '.png', '.gif', '.mp4', '.avi', '.mov'}
MAX_FILE_SIZE = 30 * 1024 * 1024  # 30MB


def validate_file(file: UploadFile) -> None:
    if file.size and file.size > MAX_FILE_SIZE:
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail=f"File size exceeds maximum limit of {MAX_FILE_SIZE // (1024 * 1024)}MB"
        )

    file_extension = os.path.splitext(file.filename)[1].lower()
    if file_extension not in ALLOWED_EXTENSIONS:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"File type not allowed. Allowed types: {', '.join(ALLOWED_EXTENSIONS)}"
        )


@files_router.post("/")
async def upload_file(file: Annotated[UploadFile, File()], current_user: Annotated[User, Depends(get_current_active_user)]):
    validate_file(file)
    saved_file = await ORM.upload_file(file, current_user.user_id)
    return FileMeta.model_validate(saved_file)

@files_router.get("/{file_id}")
async def download_file(file_id: PositiveInt):
    file = await ORM.get_file_by_id(file_id)
    if file is None:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST,
                            detail=f"File with id: {file_id} doesnt exist")
    return StreamingResponse(io.BytesIO(file.data), media_type=file.content_type,
                             headers={"Content-Disposition": f"attachment; filename={file.filename}"})